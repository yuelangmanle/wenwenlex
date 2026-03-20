package com.yueliangmanle.danci.core.pronunciation

import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.WordAudioAsset
import com.yueliangmanle.danci.core.model.WordAudioAssetStatus
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineTtsEngineTest {
    @Test
    fun speakWord_doesNotUseGlobalActivePackForAccentSpecificRequest() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ukInstallDir = File(appContext.cacheDir, "offline-tts-uk-pack").apply { mkdirs() }
        val repository = TrackingOfflineVoicePackRepository(
            globalActivePack = TestVoicePackFactory.voicePack(
                id = "en-gb-bridge-basic",
                locale = "en-GB",
                accent = PronunciationAccent.UK.storageValue,
                engineType = "system_tts_bridge",
                status = VoicePackStatus.READY.storageValue,
                installDir = ukInstallDir.absolutePath,
                isActive = true,
            ),
        )
        val engine = OfflineTtsEngine(
            voicePackRepository = repository,
            bridgeSpeaker = SystemTtsEngine(appContext),
        )

        val result = engine.speakWord(
            word = Word(id = 1L, lemma = "color"),
            accent = PronunciationAccent.US,
        )

        assertNull(result)
        assertEquals(0, repository.globalRequests)
        assertEquals(listOf(PronunciationAccent.US), repository.accentRequests)
    }

    @Test
    fun speakNativeWordIfAvailable_usesInjectedAudioPlayerForCachedNativeAudio() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val nativeInstallDir = File(appContext.cacheDir, "offline-tts-native-pack").apply { mkdirs() }
        val nativeCacheFile = File(appContext.cacheDir, "offline-tts-native-cache.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }
        val repository = TrackingOfflineVoicePackRepository(
            activeByAccent = mapOf(
                PronunciationAccent.UK to TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    accent = PronunciationAccent.UK.storageValue,
                    engineType = "sherpa_onnx",
                    status = VoicePackStatus.READY.storageValue,
                    installDir = nativeInstallDir.absolutePath,
                    isActive = true,
                    modelFamily = "kokoro",
                    version = "1.4",
                ),
            ),
        )
        val wordAudioRepository = FakeOfflineWordAudioRepository(
            nativeGeneratedAsset = WordAudioAsset(
                id = 1L,
                wordId = 1L,
                accent = PronunciationAccent.UK.storageValue,
                sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
                localPath = nativeCacheFile.absolutePath,
                status = WordAudioAssetStatus.READY.storageValue,
            ),
        )
        val playerCalls = mutableListOf<String?>()
        val engine = OfflineTtsEngine(
            voicePackRepository = repository,
            bridgeSpeaker = SystemTtsEngine(appContext),
            nativeWordTtsEngine = NativeOfflineWordTtsEngine(
                context = appContext,
                voicePackRepository = repository,
                wordAudioRepository = wordAudioRepository,
                runtimeLoader = {
                    error("cached native audio should not trigger runtime synthesis")
                },
            ),
            audioPlayer = { path ->
                playerCalls += path
                true
            },
        )

        val result = engine.speakNativeWordIfAvailable(
            word = Word(id = 1L, lemma = "abandon"),
            accent = PronunciationAccent.UK,
        )

        assertEquals(listOf(nativeCacheFile.absolutePath), playerCalls)
        assertEquals(listOf(wordAudioRepository.nativeGeneratedAsset), wordAudioRepository.markedPlayed)
        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED, result?.source)
        assertTrue(result?.cacheHit == true)
    }
}

private class TrackingOfflineVoicePackRepository(
    private val globalActivePack: VoicePack? = null,
    private val activeByAccent: Map<PronunciationAccent, VoicePack?> = emptyMap(),
) : VoicePackRepository {
    var globalRequests: Int = 0
        private set
    val accentRequests = mutableListOf<PronunciationAccent>()

    override suspend fun getAllVoicePacks(): List<VoicePack> =
        buildList {
            globalActivePack?.let(::add)
            activeByAccent.values.filterNotNull().forEach(::add)
        }.distinctBy(VoicePack::id)

    override suspend fun getVoicePack(id: String): VoicePack? =
        getAllVoicePacks().firstOrNull { it.id == id }

    override suspend fun getActiveVoicePack(): VoicePack? {
        globalRequests += 1
        return globalActivePack
    }

    override suspend fun getActiveVoicePack(accent: PronunciationAccent): VoicePack? {
        accentRequests += accent
        return when (accent) {
            PronunciationAccent.AUTO -> activeByAccent.values.filterNotNull().singleOrNull()
            else -> activeByAccent[accent]
        }
    }

    override suspend fun activateVoicePack(id: String) = Unit

    override suspend fun upsertVoicePack(voicePack: VoicePack) = Unit

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

private class FakeOfflineWordAudioRepository(
    val nativeGeneratedAsset: WordAudioAsset,
) : WordAudioRepository {
    val markedPlayed = mutableListOf<WordAudioAsset>()

    override suspend fun findCachedAsset(
        wordId: Long,
        accent: PronunciationAccent,
    ): WordAudioAsset? = null

    override suspend fun findNativeGeneratedAsset(
        wordId: Long,
        accent: PronunciationAccent,
        expectedNamespace: String?,
    ): WordAudioAsset? =
        nativeGeneratedAsset.takeIf {
            it.wordId == wordId && PronunciationAccent.fromStorageValue(it.accent) == accent
        }

    override suspend fun isRemoteLookupCoolingDown(
        wordId: Long,
        accent: PronunciationAccent,
    ): Boolean = false

    override suspend fun cacheDictionaryAudio(
        wordId: Long,
        candidate: com.yueliangmanle.danci.core.model.DictionaryAudioCandidate,
    ): WordAudioAsset? = null

    override suspend fun markRemoteLookupFailure(
        wordId: Long,
        accent: PronunciationAccent,
        errorMessage: String,
    ) = Unit

    override suspend fun markPlayed(asset: WordAudioAsset) {
        markedPlayed += asset
    }

    override suspend fun clearDictionaryCache(): Int = 0

    override suspend fun cacheSizeBytes(): Long = 0L
}
