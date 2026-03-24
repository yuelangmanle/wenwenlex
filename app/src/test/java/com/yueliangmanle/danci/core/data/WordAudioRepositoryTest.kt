package com.yueliangmanle.danci.core.data

import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.database.dao.WordAudioAssetDao
import com.yueliangmanle.danci.core.database.entity.WordAudioAssetEntity
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.WordAudioAssetStatus
import java.io.File
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordAudioRepositoryTest {
    @Test
    fun repositoryFindsNativeGeneratedAssetBeforeRemoteLookup() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = Instant.parse("2026-03-19T12:00:00Z")
        val localFile = File(appContext.filesDir, "audio-cache/generated/uk/test.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }
        val dao = FakeWordAudioAssetDao().apply {
            upsertAsset(
                WordAudioAssetEntity(
                    id = 1L,
                    wordId = 42L,
                    accent = PronunciationAccent.UK.storageValue,
                    sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
                    localPath = localFile.absolutePath,
                    mimeType = "audio/wav",
                    status = WordAudioAssetStatus.READY.storageValue,
                    fetchedAt = now,
                ),
            )
        }
        val repository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
            nowProvider = { now },
        )

        val asset = repository.findNativeGeneratedAsset(
            wordId = 42L,
            accent = PronunciationAccent.UK,
        )

        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue, asset?.sourceType)
        assertEquals(localFile.absolutePath, asset?.localPath)
    }

    @Test
    fun repositoryCachesNativeGeneratedAudioIntoManagedPath() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = Instant.parse("2026-03-19T12:00:00Z")
        val sourceFile = File(appContext.cacheDir, "native-source.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        }
        val dao = FakeWordAudioAssetDao()
        val repository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
            nowProvider = { now },
        )

        val asset = repository.cacheNativeGeneratedAudio(
            wordId = 7L,
            accent = PronunciationAccent.US,
            normalizedWord = "don't",
            sourceFile = sourceFile,
        )

        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue, asset?.sourceType)
        assertTrue(asset?.localPath.orEmpty().contains("audio-cache/generated/us/don't.wav"))
        assertTrue(asset?.localPath?.let(::File)?.exists() == true)
    }

    @Test
    fun evictToLimit_removesLeastRecentlyPlayedAssetsFirst() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = Instant.parse("2026-03-19T12:00:00Z")
        val dao = FakeWordAudioAssetDao().apply {
            upsertAsset(
                asset(
                    id = 1L,
                    wordId = 1L,
                    sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
                    localPath = audioFile(appContext, "older-dictionary.mp3", 60).absolutePath,
                    fetchedAt = now.minusSeconds(300),
                ),
            )
            upsertAsset(
                asset(
                    id = 2L,
                    wordId = 2L,
                    sourceType = PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue,
                    localPath = audioFile(appContext, "older-cloud.mp3", 50).absolutePath,
                    fetchedAt = now.minusSeconds(240),
                ),
            )
            upsertAsset(
                asset(
                    id = 3L,
                    wordId = 3L,
                    sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
                    localPath = audioFile(appContext, "newer-native.wav", 100).absolutePath,
                    fetchedAt = now.minusSeconds(60),
                ),
            )
        }
        val repository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
            nowProvider = { now },
        )

        val deleted = repository.evictToLimit(limitBytes = 100)

        assertEquals(
            listOf("older-dictionary.mp3", "older-cloud.mp3"),
            deleted.map { File(it.localPath.orEmpty()).name },
        )
        assertEquals(100L, repository.cacheSizeBytes())
    }

    @Test
    fun clearBucket_removesOnlyRequestedSourceType() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = Instant.parse("2026-03-19T12:00:00Z")
        val dao = FakeWordAudioAssetDao().apply {
            upsertAsset(
                asset(
                    id = 1L,
                    wordId = 1L,
                    sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
                    localPath = audioFile(appContext, "dictionary.mp3", 20).absolutePath,
                    fetchedAt = now.minusSeconds(120),
                ),
            )
            upsertAsset(
                asset(
                    id = 2L,
                    wordId = 2L,
                    sourceType = PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue,
                    localPath = audioFile(appContext, "cloud.mp3", 20).absolutePath,
                    fetchedAt = now.minusSeconds(60),
                ),
            )
        }
        val repository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
            nowProvider = { now },
        )

        repository.clearBucket(sourceType = PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue)

        assertEquals(
            0,
            repository.summarizeBySource()
                .firstOrNull { it.sourceType == PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue }
                ?.count ?: 0,
        )
        assertTrue(
            repository.summarizeBySource().any {
                it.sourceType == PlaybackSource.DICTIONARY_CACHE.storageValue && it.count == 1
            },
        )
    }
}

private fun asset(
    id: Long,
    wordId: Long,
    sourceType: String,
    localPath: String,
    fetchedAt: Instant,
): WordAudioAssetEntity =
    WordAudioAssetEntity(
        id = id,
        wordId = wordId,
        accent = PronunciationAccent.UK.storageValue,
        sourceType = sourceType,
        localPath = localPath,
        mimeType = "audio/mpeg",
        status = WordAudioAssetStatus.READY.storageValue,
        fetchedAt = fetchedAt,
    )

private fun audioFile(
    context: android.content.Context,
    name: String,
    sizeBytes: Int,
): File = File(context.filesDir, "audio-cache/test/$name").apply {
    parentFile?.mkdirs()
    writeBytes(ByteArray(sizeBytes) { 0x01 })
}

private class FakeWordAudioAssetDao : WordAudioAssetDao {
    private val assets = mutableListOf<WordAudioAssetEntity>()
    private var nextId = 1L

    override suspend fun upsertAsset(asset: WordAudioAssetEntity): Long {
        val resolvedId = asset.id.takeIf { it > 0 } ?: nextId++
        val entity = asset.copy(id = resolvedId)
        assets.removeAll { it.id == resolvedId }
        assets += entity
        return resolvedId
    }

    override suspend fun upsertAssets(assets: List<WordAudioAssetEntity>) {
        assets.forEach { upsertAsset(it) }
    }

    override suspend fun findLatestAsset(
        wordId: Long,
        accent: String,
        sourceType: String,
    ): WordAudioAssetEntity? =
        assets
            .filter {
                it.wordId == wordId &&
                    it.accent == accent &&
                    it.sourceType == sourceType
            }
            .sortedWith(
                compareByDescending<WordAudioAssetEntity> { it.lastPlayedAt ?: it.fetchedAt ?: Instant.EPOCH }
                    .thenByDescending(WordAudioAssetEntity::id),
            )
            .firstOrNull()

    override suspend fun findAssetsForWord(
        wordId: Long,
        sourceType: String,
        status: String,
    ): List<WordAudioAssetEntity> =
        assets
            .filter {
                it.wordId == wordId &&
                    it.sourceType == sourceType &&
                    it.status == status
            }
            .sortedWith(
                compareByDescending<WordAudioAssetEntity> { it.lastPlayedAt ?: it.fetchedAt ?: Instant.EPOCH }
                    .thenByDescending(WordAudioAssetEntity::id),
            )

    override suspend fun getAssetsBySource(
        sourceType: String,
        status: String,
    ): List<WordAudioAssetEntity> =
        assets
            .filter {
                it.sourceType == sourceType &&
                    it.status == status
            }
            .sortedWith(
                compareBy<WordAudioAssetEntity> { it.lastPlayedAt ?: it.fetchedAt ?: Instant.EPOCH }
                    .thenBy(WordAudioAssetEntity::id),
            )

    override suspend fun getAssetsByStatus(
        status: String,
    ): List<WordAudioAssetEntity> =
        assets
            .filter { it.status == status }
            .sortedWith(
                compareBy<WordAudioAssetEntity> { it.lastPlayedAt ?: it.fetchedAt ?: Instant.EPOCH }
                    .thenBy(WordAudioAssetEntity::id),
            )

    override suspend fun deleteAssetById(id: Long) {
        assets.removeAll { it.id == id }
    }

    override suspend fun deleteAssetsBySource(sourceType: String) {
        assets.removeAll { it.sourceType == sourceType }
    }

    override suspend fun clearAssets() {
        assets.clear()
    }
}
