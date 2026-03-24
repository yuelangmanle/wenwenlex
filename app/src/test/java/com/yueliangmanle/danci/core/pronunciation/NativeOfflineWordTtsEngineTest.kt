package com.yueliangmanle.danci.core.pronunciation

import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.data.RoomWordAudioRepository
import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.database.dao.WordAudioAssetDao
import com.yueliangmanle.danci.core.database.entity.WordAudioAssetEntity
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.model.Word
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
class NativeOfflineWordTtsEngineTest {
    @Test
    fun nativeEngineSynthesizesWordIntoGeneratedCacheFile() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val installDir = File(appContext.cacheDir, "native-pack-us").apply { mkdirs() }
        val now = Instant.parse("2026-03-19T12:00:00Z")
        val engine = NativeOfflineWordTtsEngine(
            context = appContext,
            voicePackRepository = FakeNativeVoicePackRepository(
                packs = mutableListOf(
                    TestVoicePackFactory.voicePack(
                        id = "en-us-offline-word-v1",
                        accent = PronunciationAccent.US.storageValue,
                        engineType = "sherpa_onnx",
                        status = VoicePackStatus.READY.storageValue,
                        installDir = installDir.absolutePath,
                        isActive = true,
                    ),
                ),
            ),
            wordAudioRepository = RoomWordAudioRepository(
                appContext = appContext,
                dao = FakeNativeWordAudioAssetDao(),
                nowProvider = { now },
            ),
            runtimeLoader = { FakeSherpaOnnxRuntime() },
        )

        val result = engine.synthesizeWord(
            word = Word(id = 7L, lemma = "Don't"),
            accent = PronunciationAccent.US,
        )

        assertTrue(result?.outputFile?.exists() == true)
        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue, result?.asset?.sourceType)
    }
}

private class FakeSherpaOnnxRuntime : SherpaOnnxRuntime {
    override fun synthesizeWord(
        text: String,
        outputFile: File,
    ) {
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes("RIFF:$text".toByteArray())
    }
}

private class FakeNativeVoicePackRepository(
    private val packs: MutableList<com.yueliangmanle.danci.core.model.VoicePack>,
) : VoicePackRepository {
    override suspend fun getAllVoicePacks(): List<com.yueliangmanle.danci.core.model.VoicePack> = packs.toList()

    override suspend fun getVoicePack(id: String): com.yueliangmanle.danci.core.model.VoicePack? =
        packs.firstOrNull { it.id == id }

    override suspend fun getActiveVoicePack(): com.yueliangmanle.danci.core.model.VoicePack? =
        packs.firstOrNull { it.isActive }

    override suspend fun activateVoicePack(id: String) = Unit

    override suspend fun upsertVoicePack(voicePack: com.yueliangmanle.danci.core.model.VoicePack) = Unit

    override suspend fun removeVoicePack(id: String) = Unit

    override suspend fun syncManifest(jsonText: String): Int = 0

    override suspend fun refreshCatalog(): Int = 0

    override suspend fun updateVoicePackStatus(
        id: String,
        status: String,
        installDir: String?,
        installedSizeBytes: Long?,
    ) = Unit

    override suspend fun markInstalled(
        id: String,
        installDir: String,
        installedSizeBytes: Long,
    ) = Unit

    override fun voicePackRootDir(): File = File("/tmp")
}

private class FakeNativeWordAudioAssetDao : WordAudioAssetDao {
    private val assets = mutableListOf<WordAudioAssetEntity>()
    private var nextId = 1L

    override suspend fun upsertAsset(asset: WordAudioAssetEntity): Long {
        val resolvedId = asset.id.takeIf { it > 0 } ?: nextId++
        assets.removeAll { it.id == resolvedId }
        assets += asset.copy(id = resolvedId)
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
            .filter { it.wordId == wordId && it.accent == accent && it.sourceType == sourceType }
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
            .filter { it.wordId == wordId && it.sourceType == sourceType && it.status == status }

    override suspend fun getAssetsBySource(
        sourceType: String,
        status: String,
    ): List<WordAudioAssetEntity> =
        assets
            .filter { it.sourceType == sourceType && it.status == status }

    override suspend fun getAssetsByStatus(
        status: String,
    ): List<WordAudioAssetEntity> =
        assets.filter { it.status == status }

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
