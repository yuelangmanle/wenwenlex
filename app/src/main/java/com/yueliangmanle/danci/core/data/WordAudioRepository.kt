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

interface WordAudioRepository {
    suspend fun findCachedAsset(wordId: Long, accent: PronunciationAccent): WordAudioAsset?
    suspend fun findNativeGeneratedAsset(
        wordId: Long,
        accent: PronunciationAccent,
        expectedNamespace: String? = null,
    ): WordAudioAsset? = null
    suspend fun cacheNativeGeneratedAudio(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        modelFamily: String,
        packVersion: String,
        sourceFile: File,
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
        dao.findLatestAsset(
            wordId,
            accent.storageValue,
            PlaybackSource.DICTIONARY_CACHE.storageValue,
        )?.let(candidates::add)
        if (accent != PronunciationAccent.AUTO) {
            dao.findLatestAsset(
                wordId,
                PronunciationAccent.AUTO.storageValue,
                PlaybackSource.DICTIONARY_CACHE.storageValue,
            )?.let(candidates::add)
        }
        return candidates
            .map(WordAudioAssetEntity::asExternalModel)
            .firstOrNull { asset ->
                asset.status == WordAudioAssetStatus.READY.storageValue &&
                    asset.localPath?.let(::File)?.exists() == true
            }
    }

    override suspend fun findNativeGeneratedAsset(
        wordId: Long,
        accent: PronunciationAccent,
        expectedNamespace: String?,
    ): WordAudioAsset? {
        val candidates = mutableListOf<WordAudioAssetEntity>()
        candidates += dao.findAssetsForWordAccentAndSource(
            wordId,
            accent.storageValue,
            PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
            WordAudioAssetStatus.READY.storageValue,
        )
        if (accent != PronunciationAccent.AUTO) {
            candidates += dao.findAssetsForWordAccentAndSource(
                wordId,
                PronunciationAccent.AUTO.storageValue,
                PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
                WordAudioAssetStatus.READY.storageValue,
            )
        }
        return candidates
            .map(WordAudioAssetEntity::asExternalModel)
            .firstOrNull { asset ->
                asset.status == WordAudioAssetStatus.READY.storageValue &&
                    asset.localPath?.let(::File)?.exists() == true &&
                    asset.matchesGeneratedNamespace(expectedNamespace)
            }
    }

    override suspend fun cacheNativeGeneratedAudio(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        modelFamily: String,
        packVersion: String,
        sourceFile: File,
        mimeType: String,
    ): WordAudioAsset? {
        if (normalizedWord.isBlank() || !sourceFile.exists()) {
            return null
        }
        val targetFile = buildGeneratedCacheFile(
            normalizedWord = normalizedWord,
            accent = accent,
            modelFamily = modelFamily,
            packVersion = packVersion,
        )
        if (sourceFile.absolutePath != targetFile.absolutePath) {
            sourceFile.copyTo(targetFile, overwrite = true)
        }
        val checksum = sha256(targetFile.readBytes())
        val existing = dao.findLatestAsset(
            wordId = wordId,
            accent = accent.storageValue,
            sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
        )
        val entity = WordAudioAssetEntity(
            id = existing?.id ?: 0L,
            wordId = wordId,
            accent = accent.storageValue,
            sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
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
        val assets = dao.getAssetsBySource(
            sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
            status = WordAudioAssetStatus.READY.storageValue,
        )
        assets.forEach { asset ->
            asset.localPath?.let(::File)?.takeIf(File::exists)?.delete()
        }
        dao.deleteAssetsBySource(PlaybackSource.DICTIONARY_CACHE.storageValue)
        return assets.size
    }

    override suspend fun cacheSizeBytes(): Long =
        dao.getAssetsBySource(
            sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
            status = WordAudioAssetStatus.READY.storageValue,
        ).sumOf { asset ->
            asset.localPath?.let(::File)?.takeIf(File::exists)?.length() ?: 0L
        }

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
        accent: PronunciationAccent,
        modelFamily: String,
        packVersion: String,
    ): File {
        val namespace = buildGeneratedNamespace(
            accent = accent,
            modelFamily = modelFamily,
            packVersion = packVersion,
        )
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
): String =
    listOf(
        accent.storageValue,
        sanitizeCacheSegment(modelFamily, fallback = "unknown-model"),
        sanitizeCacheSegment(packVersion, fallback = "unknown-version"),
    ).joinToString("/")

private fun sanitizeCacheSegment(
    value: String,
    fallback: String,
): String =
    value.trim()
        .takeIf(String::isNotBlank)
        ?.replace(Regex("""[^a-zA-Z0-9._-]+"""), "_")
        ?.lowercase()
        ?: fallback

private fun WordAudioAsset.matchesGeneratedNamespace(expectedNamespace: String?): Boolean {
    if (expectedNamespace.isNullOrBlank()) {
        return true
    }
    val normalizedPath = localPath.orEmpty().replace('\\', '/')
    return normalizedPath.contains("/audio-cache/generated/$expectedNamespace/")
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

internal fun WordAudioAssetEntity.asExternalModel(): WordAudioAsset =
    WordAudioAsset(
        id = id,
        wordId = wordId,
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
