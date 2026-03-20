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
import org.junit.Assert.assertNull
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
        val localFile = File(appContext.filesDir, "audio-cache/generated/uk/kokoro/1.4/test.wav").apply {
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
            expectedNamespace = "uk/kokoro/1.4",
        )

        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue, asset?.sourceType)
        assertEquals(localFile.absolutePath, asset?.localPath)
    }

    @Test
    fun findNativeGeneratedAssetIgnoresOldPackVersionCache() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = Instant.parse("2026-03-19T12:00:00Z")
        val oldVersionFile = File(appContext.filesDir, "audio-cache/generated/uk/kokoro/1.3/hello.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }
        val dao = FakeWordAudioAssetDao().apply {
            upsertAsset(
                WordAudioAssetEntity(
                    id = 2L,
                    wordId = 42L,
                    accent = PronunciationAccent.UK.storageValue,
                    sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
                    localPath = oldVersionFile.absolutePath,
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
            expectedNamespace = "uk/kokoro/1.4",
        )

        assertNull(asset)
    }

    @Test
    fun repositoryCachesNativeGeneratedAudioIntoVersionedManagedPath() = runTest {
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
            modelFamily = "kokoro",
            packVersion = "1.4",
            sourceFile = sourceFile,
        )

        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue, asset?.sourceType)
        assertTrue(asset?.localPath.orEmpty().contains("audio-cache/generated/us/kokoro/1.4/don't.wav"))
        assertTrue(asset?.localPath?.let(::File)?.exists() == true)
    }
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

    override suspend fun findAssetsForWordAccentAndSource(
        wordId: Long,
        accent: String,
        sourceType: String,
        status: String,
    ): List<WordAudioAssetEntity> =
        assets
            .filter {
                it.wordId == wordId &&
                    it.accent == accent &&
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
