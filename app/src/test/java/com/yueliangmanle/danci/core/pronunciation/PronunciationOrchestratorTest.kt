package com.yueliangmanle.danci.core.pronunciation

import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationSessionPreference
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.WordAudioAsset
import com.yueliangmanle.danci.core.model.WordAudioAssetStatus
import com.yueliangmanle.danci.core.model.metadataEntries
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PronunciationOrchestratorTest {
    @Test
    fun orchestratorUsesNativeGeneratedCacheBeforeRemoteDictionaryLookup() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val word = Word(id = 42L, lemma = "abandon")
        val nativeFile = File(appContext.cacheDir, "native-generated.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }
        val nativeAsset = WordAudioAsset(
            id = 1L,
            wordId = word.id,
            accent = PronunciationAccent.UK.storageValue,
            sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
            localPath = nativeFile.absolutePath,
            status = WordAudioAssetStatus.READY.storageValue,
        )
        val wordAudioRepository = FakeOrchestratorWordAudioRepository(nativeGeneratedAsset = nativeAsset)
        val dictionaryAudioService = CountingDictionaryAudioService(
            candidates = listOf(
                DictionaryAudioCandidate(
                    url = "https://example.com/abandon.mp3",
                    accent = PronunciationAccent.UK,
                ),
            ),
        )
        val systemTtsEngine = SystemTtsEngine(appContext)
        val voicePackRepository = FakeOrchestratorVoicePackRepository(
            packs = mutableListOf(
                TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    accent = PronunciationAccent.UK.storageValue,
                    engineType = "sherpa_onnx",
                    status = com.yueliangmanle.danci.core.model.VoicePackStatus.READY.storageValue,
                    installDir = File(appContext.cacheDir, "native-pack-uk").absolutePath,
                    isActive = true,
                    modelFamily = "kokoro",
                    version = "1.4",
                ),
            ),
        )
        val recorder = RecordingStudyEventRecorder()
        val orchestrator = PronunciationOrchestrator(
            settingsRepository = FakeOrchestratorSettingsRepository(),
            wordRepository = FakeOrchestratorWordRepository(word),
            wordAudioRepository = wordAudioRepository,
            voicePackRepository = voicePackRepository,
            dictionaryAudioService = dictionaryAudioService,
            offlineTtsEngine = OfflineTtsEngine(
                voicePackRepository = voicePackRepository,
                bridgeSpeaker = systemTtsEngine,
                nativeWordTtsEngine = NativeOfflineWordTtsEngine(
                    context = appContext,
                    voicePackRepository = voicePackRepository,
                    wordAudioRepository = wordAudioRepository,
                    runtimeLoader = { FakeSherpaOnnxRuntime() },
                ),
                audioPlayer = { true },
            ),
            systemTtsEngine = systemTtsEngine,
            telemetryRecorder = PlaybackTelemetryRecorder(recorder),
            audioPlayer = { true },
            nowProvider = { Instant.parse("2026-03-20T10:00:00Z") },
        )

        val result = orchestrator.playWord(
            word = word,
            accentOverride = PronunciationAccent.UK,
        )

        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED, result.source)
        assertEquals("已播放本地离线生成音频。", result.statusMessage)
        assertEquals(0, dictionaryAudioService.resolveCalls)
        assertEquals(nativeAsset, wordAudioRepository.markedPlayed.single())
        assertEquals("true", recorder.events.single().metadataEntries()["cache_hit"])
    }

    @Test
    fun orchestratorFallsBackToLaterRemoteCandidateWhenFirstDownloadFails() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val word = Word(id = 99L, lemma = "abandon")
        val remoteAsset = WordAudioAsset(
            id = 2L,
            wordId = word.id,
            accent = PronunciationAccent.UK.storageValue,
            sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
            localPath = "/tmp/remote-dictionary.wav",
            status = WordAudioAssetStatus.READY.storageValue,
        )
        val wordAudioRepository = FakeOrchestratorWordAudioRepository(
            nativeGeneratedAsset = null,
            dictionaryAssetsByUrl = mapOf(
                "https://source-b.example/abandon-uk.mp3" to remoteAsset,
            ),
        )
        val dictionaryAudioService = CountingDictionaryAudioService(
            candidates = listOf(
                DictionaryAudioCandidate(
                    url = "https://source-a.example/abandon-uk.mp3",
                    accent = PronunciationAccent.UK,
                    sourceLabel = "源A",
                ),
                DictionaryAudioCandidate(
                    url = "https://source-b.example/abandon-uk.mp3",
                    accent = PronunciationAccent.UK,
                    sourceLabel = "有道词典",
                ),
            ),
        )
        val systemTtsEngine = SystemTtsEngine(appContext)
        val voicePackRepository = FakeOrchestratorVoicePackRepository()
        val recorder = RecordingStudyEventRecorder()
        val orchestrator = PronunciationOrchestrator(
            settingsRepository = FakeOrchestratorSettingsRepository(),
            wordRepository = FakeOrchestratorWordRepository(word),
            wordAudioRepository = wordAudioRepository,
            voicePackRepository = voicePackRepository,
            dictionaryAudioService = dictionaryAudioService,
            offlineTtsEngine = OfflineTtsEngine(
                voicePackRepository = voicePackRepository,
                bridgeSpeaker = systemTtsEngine,
                audioPlayer = { true },
            ),
            systemTtsEngine = systemTtsEngine,
            telemetryRecorder = PlaybackTelemetryRecorder(recorder),
            audioPlayer = { true },
            nowProvider = { Instant.parse("2026-03-20T10:00:00Z") },
        )

        val result = orchestrator.playWord(
            word = word,
            accentOverride = PronunciationAccent.UK,
        )

        assertEquals(PlaybackSource.DICTIONARY_REMOTE, result.source)
        assertEquals("已联网获取英式词典音频（有道词典）。", result.statusMessage)
        assertEquals(
            listOf(
                "https://source-a.example/abandon-uk.mp3",
                "https://source-b.example/abandon-uk.mp3",
            ),
            wordAudioRepository.cachedCandidateUrls,
        )
        assertEquals(1, dictionaryAudioService.resolveCalls)
        assertEquals(remoteAsset, wordAudioRepository.markedPlayed.single())
        assertEquals("dictionary_remote", recorder.events.single().metadataEntries()["resolved_source"])
    }

    @Test
    fun playWord_fallsBackToDictionaryAfterNativeFailure_andRecordsFailureStage() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val word = Word(id = 18L, lemma = "colour")
        val failingNativeInstallDir = File(appContext.cacheDir, "native-pack-uk-failing").apply { mkdirs() }
        val remoteAsset = WordAudioAsset(
            id = 3L,
            wordId = word.id,
            accent = PronunciationAccent.UK.storageValue,
            sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
            localPath = "/tmp/colour-remote.wav",
            status = WordAudioAssetStatus.READY.storageValue,
        )
        val wordAudioRepository = FakeOrchestratorWordAudioRepository(
            nativeGeneratedAsset = null,
            dictionaryAssetsByUrl = mapOf(
                "https://source-b.example/colour-uk.mp3" to remoteAsset,
            ),
        )
        val dictionaryAudioService = CountingDictionaryAudioService(
            candidates = listOf(
                DictionaryAudioCandidate(
                    url = "https://source-b.example/colour-uk.mp3",
                    accent = PronunciationAccent.UK,
                    sourceLabel = "有道词典",
                ),
            ),
        )
        val voicePackRepository = FakeOrchestratorVoicePackRepository(
            packs = mutableListOf(
                TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    accent = PronunciationAccent.UK.storageValue,
                    engineType = "sherpa_onnx",
                    status = com.yueliangmanle.danci.core.model.VoicePackStatus.READY.storageValue,
                    installDir = failingNativeInstallDir.absolutePath,
                    isActive = true,
                    modelFamily = "kokoro",
                    version = "1.4",
                ),
            ),
        )
        val recorder = RecordingStudyEventRecorder()
        val orchestrator = PronunciationOrchestrator(
            settingsRepository = FakeOrchestratorSettingsRepository(),
            wordRepository = FakeOrchestratorWordRepository(word),
            wordAudioRepository = wordAudioRepository,
            voicePackRepository = voicePackRepository,
            dictionaryAudioService = dictionaryAudioService,
            offlineTtsEngine = OfflineTtsEngine(
                voicePackRepository = voicePackRepository,
                bridgeSpeaker = SystemTtsEngine(appContext),
                nativeWordTtsEngine = NativeOfflineWordTtsEngine(
                    context = appContext,
                    voicePackRepository = voicePackRepository,
                    wordAudioRepository = wordAudioRepository,
                    runtimeLoader = {
                        error("synthetic native failure")
                    },
                ),
                audioPlayer = { true },
            ),
            systemTtsEngine = SystemTtsEngine(appContext),
            telemetryRecorder = PlaybackTelemetryRecorder(recorder),
            audioPlayer = { true },
            nowProvider = { Instant.parse("2026-03-20T10:00:00Z") },
        )

        val result = orchestrator.playWord(
            word = word,
            accentOverride = PronunciationAccent.UK,
        )

        val metadata = recorder.events.single().metadataEntries()
        assertEquals(PlaybackSource.DICTIONARY_REMOTE, result.source)
        assertEquals("native_synthesis", metadata["failure_stage"])
        assertEquals("true", metadata["fallback_used"])
        assertEquals("1.4", metadata["voice_pack_version"])
        assertEquals("colour", metadata["normalized_word"])
        assertTrue(metadata["latency_ms"] != null)
    }

    @Test
    fun playWord_prefersDictionarySourceBeforeNativeWhenDefaultSourceIsDictionary() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val word = Word(id = 77L, lemma = "abandon")
        val remoteAsset = WordAudioAsset(
            id = 7L,
            wordId = word.id,
            accent = PronunciationAccent.UK.storageValue,
            sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
            localPath = "/tmp/abandon-dict.wav",
            status = WordAudioAssetStatus.READY.storageValue,
        )
        val wordAudioRepository = FakeOrchestratorWordAudioRepository(
            nativeGeneratedAsset = null,
            dictionaryAssetsByUrl = mapOf(
                "https://source-b.example/abandon-uk.mp3" to remoteAsset,
            ),
        )
        val dictionaryAudioService = CountingDictionaryAudioService(
            candidates = listOf(
                DictionaryAudioCandidate(
                    url = "https://source-b.example/abandon-uk.mp3",
                    accent = PronunciationAccent.UK,
                    sourceLabel = "有道词典",
                ),
            ),
        )
        val voicePackRepository = FakeOrchestratorVoicePackRepository(
            packs = mutableListOf(
                TestVoicePackFactory.voicePack(
                    id = "native-uk",
                    accent = PronunciationAccent.UK.storageValue,
                    engineType = "sherpa_onnx",
                    status = com.yueliangmanle.danci.core.model.VoicePackStatus.READY.storageValue,
                    installDir = File(appContext.cacheDir, "native-pack-preferred-dict").apply { mkdirs() }.absolutePath,
                    isActive = true,
                ),
            ),
        )
        val sourceRepository = FakeOrchestratorSourceRepository(
            listOf(
                orchestratorSource(
                    id = "dictionary-uk",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.UK,
                    isDefaultForWord = true,
                ),
                orchestratorSource(
                    id = "native-uk",
                    type = PronunciationSourceType.LOCAL_NATIVE,
                    accent = PronunciationAccent.UK,
                    isDefaultForLongText = true,
                    backingVoicePackId = "native-uk",
                ),
            ),
        )
        val recorder = RecordingStudyEventRecorder()
        val orchestrator = PronunciationOrchestrator(
            settingsRepository = FakeOrchestratorSettingsRepository(),
            wordRepository = FakeOrchestratorWordRepository(word),
            wordAudioRepository = wordAudioRepository,
            voicePackRepository = voicePackRepository,
            dictionaryAudioService = dictionaryAudioService,
            offlineTtsEngine = OfflineTtsEngine(
                voicePackRepository = voicePackRepository,
                bridgeSpeaker = SystemTtsEngine(appContext),
                nativeWordTtsEngine = NativeOfflineWordTtsEngine(
                    context = appContext,
                    voicePackRepository = voicePackRepository,
                    wordAudioRepository = wordAudioRepository,
                    runtimeLoader = {
                        error("native should not be attempted before dictionary")
                    },
                ),
                audioPlayer = { true },
            ),
            systemTtsEngine = SystemTtsEngine(appContext),
            telemetryRecorder = PlaybackTelemetryRecorder(recorder),
            pronunciationSourceRegistry = PronunciationSourceRegistry(
                sourceRepository = sourceRepository,
                voicePackRepository = voicePackRepository,
                settingsRepository = FakeOrchestratorSettingsRepository(),
            ),
            audioPlayer = { true },
            nowProvider = { Instant.parse("2026-03-23T10:00:00Z") },
        )

        val result = orchestrator.playWord(
            word = word,
            accentOverride = PronunciationAccent.UK,
        )

        val metadata = recorder.events.single().metadataEntries()
        assertEquals(PlaybackSource.DICTIONARY_REMOTE, result.source)
        assertEquals("dictionary-uk", result.preferredSourceId)
        assertEquals("dictionary-uk", result.actualSourceId)
        assertEquals("dictionary-uk", metadata["preferred_source_id"])
        assertEquals("dictionary-uk", metadata["actual_source_id"])
        assertTrue(metadata["failure_stage"].isNullOrBlank())
    }

    @Test
    fun playWord_prefersPreparedCloudCacheWhenDefaultSourceIsCloud() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val word = Word(id = 88L, lemma = "abandon")
        val cloudFile = File(appContext.cacheDir, "cloud-prebuilt.wav").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x12, 0x34, 0x56))
        }
        val cloudAsset = WordAudioAsset(
            id = 12L,
            wordId = word.id,
            sourceId = "cloud-mimo",
            presetId = "preset-calm",
            actualSourceType = PronunciationSourceType.CLOUD_TTS.storageValue,
            accent = PronunciationAccent.AUTO.storageValue,
            sourceType = PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue,
            localPath = cloudFile.absolutePath,
            status = WordAudioAssetStatus.READY.storageValue,
        )
        val wordAudioRepository = FakeOrchestratorWordAudioRepository(
            nativeGeneratedAsset = null,
            preparedAssetsBySourceType = mapOf(
                PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue to cloudAsset,
            ),
        )
        val dictionaryAudioService = CountingDictionaryAudioService(
            candidates = listOf(
                DictionaryAudioCandidate(
                    url = "https://source-b.example/abandon-uk.mp3",
                    accent = PronunciationAccent.UK,
                    sourceLabel = "有道词典",
                ),
            ),
        )
        val voicePackRepository = FakeOrchestratorVoicePackRepository()
        val sourceRepository = FakeOrchestratorSourceRepository(
            listOf(
                orchestratorSource(
                    id = "cloud-mimo",
                    type = PronunciationSourceType.CLOUD_TTS,
                    accent = PronunciationAccent.AUTO,
                    isDefaultForWord = true,
                    presets = listOf(
                        com.yueliangmanle.danci.core.model.PronunciationSourcePreset(
                            sourceId = "cloud-mimo",
                            presetId = "preset-calm",
                            displayName = "平静讲解",
                            voice = "default_en",
                            styleTemplate = "Calm",
                            isDefaultPreset = true,
                        ),
                    ),
                ),
            ),
        )
        val recorder = RecordingStudyEventRecorder()
        val orchestrator = PronunciationOrchestrator(
            settingsRepository = FakeOrchestratorSettingsRepository(),
            wordRepository = FakeOrchestratorWordRepository(word),
            wordAudioRepository = wordAudioRepository,
            voicePackRepository = voicePackRepository,
            dictionaryAudioService = dictionaryAudioService,
            offlineTtsEngine = OfflineTtsEngine(
                voicePackRepository = voicePackRepository,
                bridgeSpeaker = SystemTtsEngine(appContext),
                audioPlayer = { true },
            ),
            systemTtsEngine = SystemTtsEngine(appContext),
            telemetryRecorder = PlaybackTelemetryRecorder(recorder),
            pronunciationSourceRegistry = PronunciationSourceRegistry(
                sourceRepository = sourceRepository,
                voicePackRepository = voicePackRepository,
                settingsRepository = FakeOrchestratorSettingsRepository(),
            ),
            audioPlayer = { true },
            nowProvider = { Instant.parse("2026-03-23T11:00:00Z") },
        )

        val result = orchestrator.playWord(
            word = word,
            accentOverride = PronunciationAccent.UK,
        )

        val metadata = recorder.events.single().metadataEntries()
        assertEquals(PlaybackSource.ONLINE_PREBUILT_CACHE, result.source)
        assertEquals("cloud-mimo", result.preferredSourceId)
        assertEquals("cloud-mimo", result.actualSourceId)
        assertEquals(0, dictionaryAudioService.resolveCalls)
        assertEquals(cloudAsset, wordAudioRepository.markedPlayed.single())
        assertEquals("cloud-mimo", metadata["actual_source_id"])
        assertEquals("cloud_tts", metadata["actual_source_type"])
        assertEquals("true", metadata["cache_hit"])
    }
}

private fun orchestratorSource(
    id: String,
    type: PronunciationSourceType,
    accent: PronunciationAccent,
    isDefaultForWord: Boolean = false,
    isDefaultForLongText: Boolean = false,
    backingVoicePackId: String? = null,
    presets: List<com.yueliangmanle.danci.core.model.PronunciationSourcePreset> = emptyList(),
): PronunciationSource =
    PronunciationSource(
        id = id,
        name = id,
        sourceType = type.storageValue,
        accent = accent.storageValue,
        enabled = true,
        isDefaultForWord = isDefaultForWord,
        isDefaultForLongText = isDefaultForLongText,
        backingVoicePackId = backingVoicePackId,
        presets = presets,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

private class RecordingStudyEventRecorder : com.yueliangmanle.danci.core.data.StudyEventRecorder {
    val events = mutableListOf<StudyEvent>()

    override fun record(event: StudyEvent) {
        events += event
    }
}

private class FakeOrchestratorSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())
    private var sessionPreference = PronunciationSessionPreference()

    override val settings: Flow<AppSettings> = state

    override suspend fun getSettings(): AppSettings = state.value

    override suspend fun getPronunciationSessionPreference(): PronunciationSessionPreference = sessionPreference

    override suspend fun updateSessionWordPronunciationSourceId(sourceId: String?) {
        sessionPreference = sessionPreference.copy(sessionWordPronunciationSourceId = sourceId)
    }

    override suspend fun updateSessionLongTextPronunciationSourceId(sourceId: String?) {
        sessionPreference = sessionPreference.copy(sessionLongTextPronunciationSourceId = sourceId)
    }

    override suspend fun updateDailyGoal(dailyGoal: Int) = Unit
    override suspend fun updateWeeklyGoal(weeklyGoal: Int) = Unit
    override suspend fun updatePhaseName(phaseName: String?) = Unit
    override suspend fun updatePhaseTargetWords(phaseTargetWords: Int) = Unit
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

private class FakeOrchestratorSourceRepository(
    sources: List<PronunciationSource>,
) : PronunciationSourceRepository {
    private val sources = sources.toMutableList()

    override suspend fun getAllSources(): List<PronunciationSource> = sources.toList()

    override suspend fun getSource(sourceId: String): PronunciationSource? =
        sources.firstOrNull { it.id == sourceId }

    override suspend fun upsertSources(sources: List<PronunciationSource>) = Unit

    override suspend fun setDefaultWordSource(sourceId: String) = Unit

    override suspend fun setDefaultLongTextSource(sourceId: String) = Unit

    override suspend fun clearAll() = Unit
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
    private val dictionaryAssetsByUrl: Map<String, WordAudioAsset> = emptyMap(),
    private val preparedAssetsBySourceType: Map<String, WordAudioAsset> = emptyMap(),
) : WordAudioRepository {
    val markedPlayed = mutableListOf<WordAudioAsset>()
    val cachedCandidateUrls = mutableListOf<String>()

    override suspend fun findCachedAsset(
        wordId: Long,
        accent: PronunciationAccent,
    ): WordAudioAsset? = null

    override suspend fun findNativeGeneratedAsset(
        wordId: Long,
        accent: PronunciationAccent,
        expectedNamespace: String?,
    ): WordAudioAsset? = nativeGeneratedAsset

    override suspend fun findPreparedAssetWithContext(
        wordId: Long,
        accent: PronunciationAccent,
        sourceType: String,
        expectedNamespace: String?,
        sourceId: String?,
        presetId: String?,
    ): WordAudioAsset? = preparedAssetsBySourceType[sourceType]

    override suspend fun isRemoteLookupCoolingDown(
        wordId: Long,
        accent: PronunciationAccent,
    ): Boolean = false

    override suspend fun cacheDictionaryAudio(
        wordId: Long,
        candidate: DictionaryAudioCandidate,
    ): WordAudioAsset? {
        cachedCandidateUrls += candidate.url
        return dictionaryAssetsByUrl[candidate.url]
    }

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

private class FakeOrchestratorVoicePackRepository(
    private val packs: MutableList<com.yueliangmanle.danci.core.model.VoicePack> =
        mutableListOf(TestVoicePackFactory.voicePack(id = "en-gb-bridge-basic")),
) : VoicePackRepository {

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
    private val candidates: List<DictionaryAudioCandidate> = emptyList(),
) : DictionaryAudioService(baseUrl = "https://example.com/") {
    var resolveCalls: Int = 0
        private set

    override suspend fun resolveCandidates(
        word: String,
        accent: PronunciationAccent,
    ): List<DictionaryAudioCandidate> {
        resolveCalls += 1
        return candidates
    }
}
