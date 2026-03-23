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
    fun findCachedAsset_ignoresStaleAssets() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(appContext.filesDir, "audio-cache/words/9/us.mp3").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01))
        }
        val dao = FakeWordAudioAssetDao().apply {
            upsertAsset(
                WordAudioAssetEntity(
                    id = 1L,
                    wordId = 9L,
                    accent = PronunciationAccent.US.storageValue,
                    sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
                    localPath = file.absolutePath,
                    status = WordAudioAssetStatus.READY.storageValue,
                    assetState = WordAudioAssetStatus.STALE.storageValue,
                ),
            )
        }
        val repository: WordAudioRepository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
        )

        val hit = repository.findCachedAsset(
            wordId = 9L,
            accent = PronunciationAccent.US,
        )

        assertNull(hit)
    }

    @Test
    fun findCachedAsset_fallsBackToOlderReadyAssetWhenNewestEntryIsStale() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val readyFile = File(appContext.filesDir, "audio-cache/words/19/us-ready.mp3").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01))
        }
        val staleFile = File(appContext.filesDir, "audio-cache/words/19/us-stale.mp3").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x02))
        }
        val dao = FakeWordAudioAssetDao().apply {
            upsertAsset(
                WordAudioAssetEntity(
                    id = 1L,
                    wordId = 19L,
                    accent = PronunciationAccent.US.storageValue,
                    sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
                    localPath = readyFile.absolutePath,
                    status = WordAudioAssetStatus.READY.storageValue,
                    assetState = WordAudioAssetStatus.READY.storageValue,
                    fetchedAt = Instant.parse("2026-03-19T10:00:00Z"),
                ),
            )
            upsertAsset(
                WordAudioAssetEntity(
                    id = 2L,
                    wordId = 19L,
                    accent = PronunciationAccent.US.storageValue,
                    sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
                    localPath = staleFile.absolutePath,
                    status = WordAudioAssetStatus.READY.storageValue,
                    assetState = WordAudioAssetStatus.STALE.storageValue,
                    fetchedAt = Instant.parse("2026-03-19T12:00:00Z"),
                ),
            )
        }
        val repository: WordAudioRepository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
        )

        val hit = repository.findCachedAsset(
            wordId = 19L,
            accent = PronunciationAccent.US,
        )

        assertEquals(readyFile.absolutePath, hit?.localPath)
    }

    @Test
    fun findNativeGeneratedAsset_ignoresStaleAssets() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(appContext.filesDir, "audio-cache/generated/us/kokoro/1.4/stale.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x11))
        }
        val dao = FakeWordAudioAssetDao().apply {
            upsertAsset(
                WordAudioAssetEntity(
                    id = 3L,
                    wordId = 11L,
                    accent = PronunciationAccent.US.storageValue,
                    sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
                    namespace = "us/kokoro/1.4",
                    localPath = file.absolutePath,
                    status = WordAudioAssetStatus.READY.storageValue,
                    assetState = WordAudioAssetStatus.STALE.storageValue,
                ),
            )
        }
        val repository: WordAudioRepository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
        )

        val hit = repository.findNativeGeneratedAsset(
            wordId = 11L,
            accent = PronunciationAccent.US,
            expectedNamespace = "us/kokoro/1.4",
        )

        assertNull(hit)
    }

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
        val repository: WordAudioRepository = RoomWordAudioRepository(
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
        val repository: WordAudioRepository = RoomWordAudioRepository(
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
        val repository: WordAudioRepository = RoomWordAudioRepository(
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
        assertTrue(asset?.localPath.orEmpty().contains("audio-cache/generated/us/kokoro/1.4/"))
        assertTrue(asset?.localPath.orEmpty().endsWith("/don't.wav"))
        assertEquals(
            "us/kokoro/1.4/default-source/default-preset/word/${buildGeneratedContentHash("don't")}",
            asset?.namespace,
        )
        assertTrue(asset?.localPath?.let(::File)?.exists() == true)
    }

    @Test
    fun cacheNativeGeneratedAudioWithContext_keepsDifferentSourceAssetsSeparated() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = Instant.parse("2026-03-19T12:00:00Z")
        val sourceFile = File(appContext.cacheDir, "native-source-context.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x7F, 0x55, 0x33))
        }
        val dao = FakeWordAudioAssetDao()
        val repository: WordAudioRepository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
            nowProvider = { now },
        )

        val first = repository.cacheNativeGeneratedAudioWithContext(
            wordId = 18L,
            accent = PronunciationAccent.US,
            normalizedWord = "hello",
            modelFamily = "kokoro",
            packVersion = "1.4",
            sourceId = "local-native-a",
            presetId = "preset-a",
            sourceFile = sourceFile,
        )
        val second = repository.cacheNativeGeneratedAudioWithContext(
            wordId = 18L,
            accent = PronunciationAccent.US,
            normalizedWord = "hello",
            modelFamily = "kokoro",
            packVersion = "1.4",
            sourceId = "local-native-b",
            presetId = "preset-b",
            sourceFile = sourceFile,
        )

        val allAssets = dao.snapshotAssets()
        assertEquals(2, allAssets.size)
        assertTrue(allAssets.map { it.id }.toSet().size == 2)
        assertTrue(allAssets.any { it.sourceId == "local-native-a" && it.presetId == "preset-a" })
        assertTrue(allAssets.any { it.sourceId == "local-native-b" && it.presetId == "preset-b" })
        assertTrue(first?.namespace.orEmpty().contains("/local-native-a--"))
        assertTrue(first?.namespace.orEmpty().contains("/preset-a--"))
        assertTrue(first?.namespace.orEmpty().contains("/word/"))
        assertTrue(first?.namespace.orEmpty().endsWith(buildGeneratedContentHash("hello")))
        assertTrue(second?.namespace.orEmpty().contains("/local-native-b--"))
        assertTrue(second?.namespace.orEmpty().contains("/preset-b--"))
        assertTrue(first?.namespace != second?.namespace)
        assertTrue(first?.localPath != second?.localPath)
    }

    @Test
    fun cacheNativeGeneratedAudioWithContext_reusesSameAssetForSameContext() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = Instant.parse("2026-03-19T12:00:00Z")
        val firstSource = File(appContext.cacheDir, "native-source-same-a.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }
        val secondSource = File(appContext.cacheDir, "native-source-same-b.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x04, 0x05, 0x06))
        }
        val dao = FakeWordAudioAssetDao()
        val repository: WordAudioRepository = RoomWordAudioRepository(
            appContext = appContext,
            dao = dao,
            nowProvider = { now },
        )

        val first = repository.cacheNativeGeneratedAudioWithContext(
            wordId = 20L,
            accent = PronunciationAccent.US,
            normalizedWord = "hello",
            modelFamily = "kokoro",
            packVersion = "1.4",
            sourceId = "local-native-a",
            presetId = "preset-a",
            sourceFile = firstSource,
        )
        val second = repository.cacheNativeGeneratedAudioWithContext(
            wordId = 20L,
            accent = PronunciationAccent.US,
            normalizedWord = "hello",
            modelFamily = "kokoro",
            packVersion = "1.4",
            sourceId = "local-native-a",
            presetId = "preset-a",
            sourceFile = secondSource,
        )

        val allAssets = dao.snapshotAssets()
        assertEquals(1, allAssets.size)
        assertEquals(first?.id, second?.id)
        assertEquals(first?.namespace, second?.namespace)
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

    override suspend fun findLatestAssetByContext(
        wordId: Long,
        accent: String,
        sourceType: String,
        status: String,
        sourceId: String?,
        presetId: String?,
        namespace: String?,
    ): WordAudioAssetEntity? =
        assets
            .filter {
                it.wordId == wordId &&
                    it.accent == accent &&
                    it.sourceType == sourceType &&
                    it.status == status &&
                    it.sourceId == sourceId &&
                    it.presetId == presetId &&
                    it.namespace == namespace
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

    override suspend fun getAllAssets(): List<WordAudioAssetEntity> =
        snapshotAssets()

    override suspend fun deleteAssetById(id: Long) {
        assets.removeAll { it.id == id }
    }

    override suspend fun deleteAssetsBySource(sourceType: String) {
        assets.removeAll { it.sourceType == sourceType }
    }

    override suspend fun clearAssets() {
        assets.clear()
    }

    fun snapshotAssets(): List<WordAudioAssetEntity> =
        assets.sortedBy(WordAudioAssetEntity::id)
}
