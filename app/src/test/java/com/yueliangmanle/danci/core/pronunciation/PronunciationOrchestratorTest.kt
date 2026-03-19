package com.yueliangmanle.danci.core.pronunciation

import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.WordAudioAsset
import com.yueliangmanle.danci.core.model.WordAudioAssetStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PronunciationOrchestratorTest {
    @Test
    fun orchestratorUsesNativeGeneratedCacheBeforeRemoteDictionaryLookup() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val word = Word(id = 42L, lemma = "abandon")
        val nativeAsset = WordAudioAsset(
            id = 1L,
            wordId = word.id,
            accent = PronunciationAccent.UK.storageValue,
            sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
            localPath = "/tmp/native-generated.wav",
            status = WordAudioAssetStatus.READY.storageValue,
        )
        val wordAudioRepository = FakeOrchestratorWordAudioRepository(nativeGeneratedAsset = nativeAsset)
        val dictionaryAudioService = CountingDictionaryAudioService(
            candidate = DictionaryAudioCandidate(
                url = "https://example.com/abandon.mp3",
                accent = PronunciationAccent.UK,
            ),
        )
        val systemTtsEngine = SystemTtsEngine(appContext)
        val orchestrator = PronunciationOrchestrator(
            settingsRepository = FakeOrchestratorSettingsRepository(),
            wordRepository = FakeOrchestratorWordRepository(word),
            wordAudioRepository = wordAudioRepository,
            dictionaryAudioService = dictionaryAudioService,
            offlineTtsEngine = OfflineTtsEngine(
                voicePackRepository = FakeOrchestratorVoicePackRepository(),
                bridgeSpeaker = systemTtsEngine,
            ),
            systemTtsEngine = systemTtsEngine,
            telemetryRecorder = PlaybackTelemetryRecorder(NoOpStudyEventRecorder),
            audioPlayer = { true },
        )

        val result = orchestrator.playWord(
            word = word,
            accentOverride = PronunciationAccent.UK,
        )

        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED, result.source)
        assertEquals("已播放本地离线生成音频。", result.statusMessage)
        assertEquals(0, dictionaryAudioService.resolveCalls)
        assertEquals(nativeAsset, wordAudioRepository.markedPlayed.single())
    }
}

private class FakeOrchestratorSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())

    override val settings: Flow<AppSettings> = state

    override suspend fun getSettings(): AppSettings = state.value

    override suspend fun updateDailyGoal(dailyGoal: Int) = Unit
    override suspend fun updateActiveBookId(bookId: String?) = Unit
    override suspend fun updateAiEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiBaseUrl(baseUrl: String) = Unit
    override suspend fun updateAiModel(model: String) = Unit
    override suspend fun updateDefaultAiProfileId(profileId: String?) = Unit
    override suspend fun updateWordHelpProfileId(profileId: String?) = Unit
    override suspend fun updatePlanAdjustmentProfileId(profileId: String?) = Unit
    override suspend fun updatePhoneticFillProfileId(profileId: String?) = Unit
    override suspend fun updateAiPlanAdjustmentEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiSessionCheckpointEnabled(enabled: Boolean) = Unit
    override suspend fun updatePreferredPronunciationAccent(accent: String) = Unit
    override suspend fun updatePronunciationMode(mode: String) = Unit
    override suspend fun updateAllowCellularVoicePackDownload(enabled: Boolean) = Unit
    override suspend fun updateAutoCacheWordAudio(enabled: Boolean) = Unit
    override suspend fun updateAudioCacheLimitMb(limitMb: Int) = Unit
    override suspend fun updateActiveVoicePackId(voicePackId: String?) = Unit
    override suspend fun updateFallbackToSystemTts(enabled: Boolean) = Unit
    override suspend fun updatePreferOfflineForLongText(enabled: Boolean) = Unit
    override suspend fun updateReminderEnabled(enabled: Boolean) = Unit
    override suspend fun updateReminderTime(hour: Int, minute: Int) = Unit
}

private class FakeOrchestratorWordRepository(
    private val word: Word,
) : WordRepository {
    override fun observeWords(query: String): Flow<List<Word>> = flowOf(listOf(word))

    override suspend fun getWord(wordId: Long): Word? = word.takeIf { it.id == wordId }

    override suspend fun getWords(wordIds: List<Long>): List<Word> = listOfNotNull(getWord(word.id))

    override suspend fun getAllWords(): List<Word> = listOf(word)

    override suspend fun insertWord(word: Word): Long = word.id

    override suspend fun updateWord(word: Word) = Unit

    override suspend fun importWords(words: List<com.yueliangmanle.danci.core.importer.ImportedWord>): List<Long> =
        emptyList()
}

private class FakeOrchestratorWordAudioRepository(
    private val nativeGeneratedAsset: WordAudioAsset?,
) : WordAudioRepository {
    val markedPlayed = mutableListOf<WordAudioAsset>()

    override suspend fun findCachedAsset(
        wordId: Long,
        accent: PronunciationAccent,
    ): WordAudioAsset? = null

    override suspend fun findNativeGeneratedAsset(
        wordId: Long,
        accent: PronunciationAccent,
    ): WordAudioAsset? = nativeGeneratedAsset

    override suspend fun isRemoteLookupCoolingDown(
        wordId: Long,
        accent: PronunciationAccent,
    ): Boolean = false

    override suspend fun cacheDictionaryAudio(
        wordId: Long,
        candidate: DictionaryAudioCandidate,
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

private class FakeOrchestratorVoicePackRepository : VoicePackRepository {
    private val packs = mutableListOf(TestVoicePackFactory.voicePack(id = "en-gb-bridge-basic"))

    override suspend fun getAllVoicePacks(): List<com.yueliangmanle.danci.core.model.VoicePack> = packs

    override suspend fun getVoicePack(id: String): com.yueliangmanle.danci.core.model.VoicePack? =
        packs.firstOrNull { it.id == id }

    override suspend fun getActiveVoicePack(): com.yueliangmanle.danci.core.model.VoicePack? = null

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

    override fun voicePackRootDir(): java.io.File = java.io.File("/tmp")
}

private class CountingDictionaryAudioService(
    private val candidate: DictionaryAudioCandidate?,
) : DictionaryAudioService(baseUrl = "https://example.com/") {
    var resolveCalls: Int = 0
        private set

    override suspend fun resolveCandidate(
        word: String,
        accent: PronunciationAccent,
    ): DictionaryAudioCandidate? {
        resolveCalls += 1
        return candidate
    }
}
