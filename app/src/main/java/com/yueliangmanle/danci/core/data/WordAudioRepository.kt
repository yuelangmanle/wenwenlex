package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.WordAudioAssetDao
import com.yueliangmanle.danci.core.database.entity.WordAudioAssetEntity
import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.WordAudioAsset
import com.yueliangmanle.danci.core.model.WordAudioAssetStatus
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val REMOTE_LOOKUP_COOLDOWN: Duration = Duration.ofDays(7)

data class AudioCacheBucketSummary(
    val sourceType: String,
    val title: String,
    val itemCount: Int,
    val sizeBytes: Long,
)

interface WordAudioRepository {
    suspend fun findCachedAsset(wordId: Long, accent: PronunciationAccent): WordAudioAsset?
    suspend fun findPreparedAssetWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        sourceType: String,
        expectedNamespace: String? = null,
        sourceId: String? = null,
        presetId: String? = null,
    ): WordAudioAsset? = null
    suspend fun findNativeGeneratedAsset(
        wordId: Long,
        accent: PronunciationAccent,
        expectedNamespace: String? = null,
    ): WordAudioAsset? = null
    suspend fun findNativeGeneratedAssetWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        expectedNamespace: String? = null,
        sourceId: String? = null,
        presetId: String? = null,
    ): WordAudioAsset? =
        findNativeGeneratedAsset(
            wordId = wordId,
            accent = accent,
            expectedNamespace = expectedNamespace,
        )

    suspend fun cacheNativeGeneratedAudio(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        modelFamily: String,
        packVersion: String,
        sourceFile: File,
        mimeType: String = "audio/wav",
    ): WordAudioAsset? = null
    suspend fun cacheNativeGeneratedAudioWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        modelFamily: String,
        packVersion: String,
        sourceId: String?,
        presetId: String?,
        sourceFile: File,
        namespace: String? = null,
        mimeType: String = "audio/wav",
    ): WordAudioAsset? =
        cacheNativeGeneratedAudio(
            wordId = wordId,
            accent = accent,
            normalizedWord = normalizedWord,
            modelFamily = modelFamily,
            packVersion = packVersion,
            sourceFile = sourceFile,
            mimeType = mimeType,
        )
    suspend fun cacheGeneratedAudioWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        playbackSource: PlaybackSource,
        actualSourceType: String?,
        modelFamily: String,
        versionTag: String,
        sourceId: String?,
        presetId: String?,
        sourceFile: File,
        namespace: String? = null,
        taskId: String? = null,
        mimeType: String = "audio/wav",
    ): WordAudioAsset? = null
    suspend fun isRemoteLookupCoolingDown(wordId: Long, accent: PronunciationAccent): Boolean
    suspend fun cacheDictionaryAudio(
        wordId: Long,
        candidate: DictionaryAudioCandidate,
    ): WordAudioAsset?
    suspend fun markRemoteLookupFailure(
        wordId: Long,
        accent: PronunciationAccent,
        errorMessage: String,
    )
    suspend fun markPlayed(asset: WordAudioAsset)
    suspend fun clearDictionaryCache(): Int
    suspend fun clearCacheBucket(sourceType: String): Int = 0
    suspend fun summarizeCacheBuckets(): List<AudioCacheBucketSummary> = emptyList()
    suspend fun cacheSizeBytes(): Long
}

class RoomWordAudioRepository(
    private val appContext: Context,
    private val dao: WordAudioAssetDao,
    private val nowProvider: () -> Instant = { Instant.now() },
) : WordAudioRepository {
    override suspend fun findCachedAsset(
        wordId: Long,
        accent: PronunciationAccent,
    ): WordAudioAsset? {
        val candidates = mutableListOf<WordAudioAssetEntity>()
        candidates += dao.findAssetsForWordAccentAndSource(
            wordId,
            accent.storageValue,
            PlaybackSource.DICTIONARY_CACHE.storageValue,
            WordAudioAssetStatus.READY.storageValue,
        )
        if (accent != PronunciationAccent.AUTO) {
            candidates += dao.findAssetsForWordAccentAndSource(
                wordId,
                PronunciationAccent.AUTO.storageValue,
                PlaybackSource.DICTIONARY_CACHE.storageValue,
                WordAudioAssetStatus.READY.storageValue,
            )
        }
        return candidates
            .map(WordAudioAssetEntity::asExternalModel)
            .firstOrNull(::isReadyLocalAsset)
    }

    override suspend fun findNativeGeneratedAsset(
        wordId: Long,
        accent: PronunciationAccent,
        expectedNamespace: String?,
    ): WordAudioAsset? =
        findPreparedAssetWithContext(
            wordId = wordId,
            accent = accent,
            sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
            expectedNamespace = expectedNamespace,
            sourceId = null,
            presetId = null,
        )

    override suspend fun findPreparedAssetWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        sourceType: String,
        expectedNamespace: String?,
        sourceId: String?,
        presetId: String?,
    ): WordAudioAsset? {
        val candidates = mutableListOf<WordAudioAssetEntity>()
        candidates += dao.findAssetsForWordAccentAndSource(
            wordId,
            accent.storageValue,
            sourceType,
            WordAudioAssetStatus.READY.storageValue,
        )
        if (accent != PronunciationAccent.AUTO) {
            candidates += dao.findAssetsForWordAccentAndSource(
                wordId,
                PronunciationAccent.AUTO.storageValue,
                sourceType,
                WordAudioAssetStatus.READY.storageValue,
            )
        }
        return candidates
            .map(WordAudioAssetEntity::asExternalModel)
            .firstOrNull { asset ->
                asset.status == WordAudioAssetStatus.READY.storageValue &&
                    asset.assetState == WordAudioAssetStatus.READY.storageValue &&
                    asset.localPath?.let(::File)?.exists() == true &&
                    asset.matchesGeneratedNamespace(expectedNamespace) &&
                    asset.matchesSourceContext(sourceId = sourceId, presetId = presetId)
            }
    }

    override suspend fun findNativeGeneratedAssetWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        expectedNamespace: String?,
        sourceId: String?,
        presetId: String?,
    ): WordAudioAsset? =
        findPreparedAssetWithContext(
            wordId = wordId,
            accent = accent,
            sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
            expectedNamespace = expectedNamespace,
            sourceId = sourceId,
            presetId = presetId,
        )

    override suspend fun cacheNativeGeneratedAudio(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        modelFamily: String,
        packVersion: String,
        sourceFile: File,
        mimeType: String,
    ): WordAudioAsset? =
        cacheNativeGeneratedAudioWithContext(
            wordId = wordId,
            accent = accent,
            normalizedWord = normalizedWord,
            modelFamily = modelFamily,
            packVersion = packVersion,
            sourceId = null,
            presetId = null,
            sourceFile = sourceFile,
            namespace = null,
            mimeType = mimeType,
        )

    override suspend fun cacheNativeGeneratedAudioWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        modelFamily: String,
        packVersion: String,
        sourceId: String?,
        presetId: String?,
        sourceFile: File,
        namespace: String?,
        mimeType: String,
    ): WordAudioAsset? {
        return cacheGeneratedAudioWithContext(
            wordId = wordId,
            accent = accent,
            normalizedWord = normalizedWord,
            playbackSource = PlaybackSource.OFFLINE_NATIVE_GENERATED,
            actualSourceType = sourceId?.let { ACTUAL_SOURCE_TYPE_LOCAL_NATIVE },
            modelFamily = modelFamily,
            versionTag = packVersion,
            sourceId = sourceId,
            presetId = presetId,
            sourceFile = sourceFile,
            namespace = namespace,
            taskId = null,
            mimeType = mimeType,
        )
    }

    override suspend fun cacheGeneratedAudioWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        playbackSource: PlaybackSource,
        actualSourceType: String?,
        modelFamily: String,
        versionTag: String,
        sourceId: String?,
        presetId: String?,
        sourceFile: File,
        namespace: String?,
        taskId: String?,
        mimeType: String,
    ): WordAudioAsset? {
        if (normalizedWord.isBlank() || !sourceFile.exists()) {
            return null
        }
        val resolvedNamespace = namespace?.takeIf(String::isNotBlank) ?: buildGeneratedNamespace(
            accent = accent,
            modelFamily = modelFamily,
            packVersion = versionTag,
            sourceId = sourceId,
            presetId = presetId,
            sceneType = GENERATED_AUDIO_SCENE_WORD,
            contentHash = buildGeneratedContentHash(normalizedWord),
        )
        val targetFile = buildGeneratedCacheFile(
            normalizedWord = normalizedWord,
            namespace = resolvedNamespace,
        )
        if (sourceFile.absolutePath != targetFile.absolutePath) {
            sourceFile.copyTo(targetFile, overwrite = true)
        }
        val checksum = sha256(targetFile.readBytes())
        val existing = dao.findLatestAssetByContext(
            wordId = wordId,
            accent = accent.storageValue,
            sourceType = playbackSource.storageValue,
            status = WordAudioAssetStatus.READY.storageValue,
            sourceId = sourceId,
            presetId = presetId,
            namespace = resolvedNamespace,
        )
        val entity = WordAudioAssetEntity(
            id = existing?.id ?: 0L,
            wordId = wordId,
            sourceId = sourceId,
            presetId = presetId,
            actualSourceType = actualSourceType,
            namespace = resolvedNamespace,
            assetState = WordAudioAssetStatus.READY.storageValue,
            taskId = taskId,
            accent = accent.storageValue,
            sourceType = playbackSource.storageValue,
            remoteUrl = null,
            localPath = targetFile.absolutePath,
            mimeType = mimeType,
            checksum = checksum,
            status = WordAudioAssetStatus.READY.storageValue,
            fetchedAt = nowProvider(),
            lastPlayedAt = existing?.lastPlayedAt,
            lastError = null,
            failureCount = 0,
        )
        val insertedId = dao.upsertAsset(entity)
        return entity.copy(id = insertedId.takeIf { it > 0 } ?: entity.id).asExternalModel()
    }

    override suspend fun isRemoteLookupCoolingDown(
        wordId: Long,
        accent: PronunciationAccent,
    ): Boolean {
        val failure = dao.findLatestAsset(
            wordId = wordId,
            accent = accent.storageValue,
            sourceType = PlaybackSource.DICTIONARY_REMOTE.storageValue,
        ) ?: return false
        if (failure.status != WordAudioAssetStatus.FAILED.storageValue || failure.fetchedAt == null) {
            return false
        }
        return Duration.between(failure.fetchedAt, nowProvider()) < REMOTE_LOOKUP_COOLDOWN
    }

    override suspend fun cacheDictionaryAudio(
        wordId: Long,
        candidate: DictionaryAudioCandidate,
    ): WordAudioAsset? {
        val targetFile = buildCacheFile(wordId, candidate.accent)
        return runCatching {
            val checksum = downloadToFile(candidate.url, targetFile)
            val existing = dao.findLatestAsset(
                wordId = wordId,
                accent = candidate.accent.storageValue,
                sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
            )
            val entity = WordAudioAssetEntity(
                id = existing?.id ?: 0L,
                wordId = wordId,
                accent = candidate.accent.storageValue,
                sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
                remoteUrl = candidate.url,
                localPath = targetFile.absolutePath,
                mimeType = candidate.mimeType ?: "audio/mpeg",
                checksum = checksum,
                status = WordAudioAssetStatus.READY.storageValue,
                fetchedAt = nowProvider(),
                lastPlayedAt = existing?.lastPlayedAt,
                lastError = null,
                failureCount = 0,
            )
            val insertedId = dao.upsertAsset(entity)
            entity.copy(id = insertedId.takeIf { it > 0 } ?: entity.id).asExternalModel()
        }.getOrNull()
    }

    override suspend fun markRemoteLookupFailure(
        wordId: Long,
        accent: PronunciationAccent,
        errorMessage: String,
    ) {
        val existing = dao.findLatestAsset(
            wordId = wordId,
            accent = accent.storageValue,
            sourceType = PlaybackSource.DICTIONARY_REMOTE.storageValue,
        )
        dao.upsertAsset(
            WordAudioAssetEntity(
                id = existing?.id ?: 0L,
                wordId = wordId,
                accent = accent.storageValue,
                sourceType = PlaybackSource.DICTIONARY_REMOTE.storageValue,
                remoteUrl = existing?.remoteUrl,
                localPath = existing?.localPath,
                mimeType = existing?.mimeType,
                checksum = existing?.checksum,
                status = WordAudioAssetStatus.FAILED.storageValue,
                fetchedAt = nowProvider(),
                lastPlayedAt = existing?.lastPlayedAt,
                lastError = errorMessage,
                failureCount = (existing?.failureCount ?: 0) + 1,
            ),
        )
    }

    override suspend fun markPlayed(asset: WordAudioAsset) {
        dao.upsertAsset(
            asset.copy(lastPlayedAt = nowProvider()).asEntity(),
        )
    }

    override suspend fun clearDictionaryCache(): Int {
        return clearCacheBucket(PlaybackSource.DICTIONARY_CACHE.storageValue)
    }

    override suspend fun clearCacheBucket(sourceType: String): Int {
        if (!isManagedLocalCacheSource(sourceType)) {
            return 0
        }
        val assets = dao.getAssetsBySource(
            sourceType = sourceType,
            status = WordAudioAssetStatus.READY.storageValue,
        )
        assets.forEach { asset ->
            asset.localPath?.let(::File)?.takeIf { it.exists() }?.delete()
        }
        dao.deleteAssetsBySource(sourceType)
        return assets.size
    }

    override suspend fun summarizeCacheBuckets(): List<AudioCacheBucketSummary> =
        MANAGED_LOCAL_CACHE_SOURCES.mapNotNull { source ->
            val assets = dao.getAssetsBySource(
                sourceType = source.storageValue,
                status = WordAudioAssetStatus.READY.storageValue,
            )
            val readyLocalAssets = assets.filter { asset ->
                asset.localPath?.let(::File)?.exists() == true
            }
            if (readyLocalAssets.isEmpty()) {
                return@mapNotNull null
            }
            AudioCacheBucketSummary(
                sourceType = source.storageValue,
                title = source.label,
                itemCount = readyLocalAssets.size,
                sizeBytes = readyLocalAssets.sumOf { asset ->
                    asset.localPath?.let(::File)?.length() ?: 0L
                },
            )
        }

    override suspend fun cacheSizeBytes(): Long =
        summarizeCacheBuckets().sumOf(AudioCacheBucketSummary::sizeBytes)

    private suspend fun downloadToFile(
        remoteUrl: String,
        targetFile: File,
    ): String = withContext(Dispatchers.IO) {
        targetFile.parentFile?.mkdirs()
        val connection = URL(remoteUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = true
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("音频下载失败：HTTP ${connection.responseCode}")
            }
            val bytes = connection.inputStream.use { it.readBytes() }
            targetFile.writeBytes(bytes)
            sha256(bytes)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildCacheFile(
        wordId: Long,
        accent: PronunciationAccent,
    ): File = File(
        appContext.filesDir,
        "audio-cache/words/$wordId/${accent.storageValue}.mp3",
    )

    private fun buildGeneratedCacheFile(
        normalizedWord: String,
        namespace: String,
    ): File {
        val target = File(
            appContext.filesDir,
            "audio-cache/generated/$namespace/$normalizedWord.wav",
        )
        target.parentFile?.mkdirs()
        return target
    }
}

internal fun buildGeneratedNamespace(
    accent: PronunciationAccent,
    modelFamily: String,
    packVersion: String,
    sourceId: String? = null,
    presetId: String? = null,
    sceneType: String = GENERATED_AUDIO_SCENE_WORD,
    contentHash: String? = null,
): String =
    listOf(
        accent.storageValue,
        sanitizeCacheSegment(modelFamily, fallback = "unknown-model"),
        sanitizeCacheSegment(packVersion, fallback = "unknown-version"),
        buildContextNamespaceSegment(sourceId, fallback = "default-source"),
        buildContextNamespaceSegment(presetId, fallback = "default-preset"),
        sanitizeCacheSegment(sceneType, fallback = GENERATED_AUDIO_SCENE_WORD),
        sanitizeCacheSegment(contentHash.orEmpty(), fallback = "shared-content"),
    ).filterNotNull().joinToString("/")

internal fun buildGeneratedContentHash(content: String): String =
    sha256(content.toByteArray()).take(GENERATED_AUDIO_HASH_LENGTH)

private fun sanitizeCacheSegment(
    value: String,
    fallback: String,
): String =
    value.trim()
        .takeIf(String::isNotBlank)
        ?.replace(Regex("""[^a-zA-Z0-9._-]+"""), "_")
        ?.lowercase()
        ?: fallback

private fun buildContextNamespaceSegment(
    rawValue: String?,
    fallback: String,
): String {
    val trimmedValue = rawValue?.trim().orEmpty()
    if (trimmedValue.isBlank()) {
        return fallback
    }
    val readableSegment = sanitizeCacheSegment(trimmedValue, fallback = fallback)
    val fingerprint = sha256(trimmedValue.toByteArray()).take(GENERATED_CONTEXT_HASH_LENGTH)
    return "$readableSegment--$fingerprint"
}

private fun isReadyLocalAsset(asset: WordAudioAsset): Boolean =
    asset.status == WordAudioAssetStatus.READY.storageValue &&
        asset.assetState == WordAudioAssetStatus.READY.storageValue &&
        asset.localPath?.let(::File)?.exists() == true

private fun WordAudioAsset.matchesGeneratedNamespace(expectedNamespace: String?): Boolean {
    if (expectedNamespace.isNullOrBlank()) {
        return true
    }
    if (namespace == expectedNamespace) {
        return true
    }
    val normalizedPath = localPath.orEmpty().replace('\\', '/')
    return normalizedPath.contains("/audio-cache/generated/$expectedNamespace/")
}

private fun WordAudioAsset.matchesSourceContext(
    sourceId: String?,
    presetId: String?,
): Boolean {
    val sourceMatched = sourceId == null || this.sourceId == sourceId
    val presetMatched = presetId == null || this.presetId == presetId
    return sourceMatched && presetMatched
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

private fun isManagedLocalCacheSource(sourceType: String): Boolean =
    MANAGED_LOCAL_CACHE_SOURCES.any { it.storageValue == sourceType }

internal fun WordAudioAssetEntity.asExternalModel(): WordAudioAsset =
    WordAudioAsset(
        id = id,
        wordId = wordId,
        sourceId = sourceId,
        presetId = presetId,
        actualSourceType = actualSourceType,
        namespace = namespace,
        assetState = assetState,
        taskId = taskId,
        accent = accent,
        sourceType = sourceType,
        remoteUrl = remoteUrl,
        localPath = localPath,
        mimeType = mimeType,
        checksum = checksum,
        status = status,
        fetchedAt = fetchedAt,
        lastPlayedAt = lastPlayedAt,
        lastError = lastError,
        failureCount = failureCount,
    )

internal fun WordAudioAsset.asEntity(): WordAudioAssetEntity =
    WordAudioAssetEntity(
        id = id,
        wordId = wordId,
        sourceId = sourceId,
        presetId = presetId,
        actualSourceType = actualSourceType,
        namespace = namespace,
        assetState = assetState,
        taskId = taskId,
        accent = accent,
        sourceType = sourceType,
        remoteUrl = remoteUrl,
        localPath = localPath,
        mimeType = mimeType,
        checksum = checksum,
        status = status,
        fetchedAt = fetchedAt,
        lastPlayedAt = lastPlayedAt,
        lastError = lastError,
        failureCount = failureCount,
    )

fun buildWordAudioRepository(context: Context): WordAudioRepository =
    RoomWordAudioRepository(
        appContext = context.applicationContext,
        dao = buildDanciDatabase(context.applicationContext).wordAudioAssetDao(),
    )

private const val GENERATED_AUDIO_SCENE_WORD = "word"
private const val GENERATED_AUDIO_HASH_LENGTH = 16
private const val GENERATED_CONTEXT_HASH_LENGTH = 10
private const val ACTUAL_SOURCE_TYPE_LOCAL_NATIVE = "local_native"

private val MANAGED_LOCAL_CACHE_SOURCES = listOf(
    PlaybackSource.DICTIONARY_CACHE,
    PlaybackSource.OFFLINE_NATIVE_CACHE,
    PlaybackSource.OFFLINE_NATIVE_GENERATED,
    PlaybackSource.ONLINE_PREBUILT_CACHE,
    PlaybackSource.OFFLINE_TTS,
)
