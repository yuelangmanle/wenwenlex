package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationSessionPreference
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.model.WordAudioAsset
import com.yueliangmanle.danci.core.pronunciation.PronunciationSourceRegistry
import com.yueliangmanle.danci.core.worker.VoicePackDownloadController
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationSettingsViewModelTest {
    @Test
    fun loadUiState_mapsSourcesAndHighlightsDefaults() = runTest {
        val settingsRepository = FakeSettingsRepository(initial = AppSettings())
        val sourceRepository = FakePronunciationSourceRepository(
            mutableListOf(
                testSource(
                    id = "dictionary-uk",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.UK,
                    isDefaultForWord = true,
                ),
                testSource(
                    id = "dictionary-us",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.US,
                ),
                testSource(
                    id = "native-us",
                    type = PronunciationSourceType.LOCAL_NATIVE,
                    accent = PronunciationAccent.US,
                    isDefaultForLongText = true,
                    backingVoicePackId = "native-us",
                ),
            ),
        )
        val voicePackRepository = FakeVoicePackRepository(
            mutableListOf(
                TestVoicePackFactory.voicePack(
                    id = "native-us",
                    name = "美式原生离线包",
                    locale = "en-US",
                    accent = "us",
                    engineType = "sherpa_onnx",
                    status = VoicePackStatus.READY.storageValue,
                    isActive = true,
                    supportsImportedWords = true,
                ),
            ),
        )
        val viewModel = PronunciationSettingsViewModel(
            settingsRepository = settingsRepository,
            wordAudioRepository = FakeWordAudioRepository(),
            voicePackRepository = voicePackRepository,
            voicePackDownloadController = FakeVoicePackDownloadController(),
            pronunciationSourceRepository = sourceRepository,
            pronunciationSourceRegistry = PronunciationSourceRegistry(
                sourceRepository = sourceRepository,
                voicePackRepository = voicePackRepository,
                settingsRepository = settingsRepository,
            ),
        )

        val state = viewModel.loadUiState()

        assertEquals(3, state.sourceItems.size)
        assertEquals("词典发音（英式）", state.defaultWordSourceLabel)
        assertEquals("美式原生离线包", state.defaultLongTextSourceLabel)
        assertTrue(state.sourceItems.single { it.id == "dictionary-uk" }.isDefaultForWord)
        assertTrue(state.sourceItems.single { it.id == "native-us" }.isDefaultForLongText)
        assertTrue(state.sourceItems.single { it.id == "native-us" }.subtitle.contains("本地原生"))
    }

    @Test
    fun setDefaultWordSource_updatesSourceFlags() = runTest {
        val settingsRepository = FakeSettingsRepository(initial = AppSettings())
        val sourceRepository = FakePronunciationSourceRepository(
            mutableListOf(
                testSource(
                    id = "dictionary-uk",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.UK,
                    isDefaultForWord = true,
                ),
                testSource(
                    id = "native-us",
                    type = PronunciationSourceType.LOCAL_NATIVE,
                    accent = PronunciationAccent.US,
                    backingVoicePackId = "native-us",
                ),
            ),
        )
        val voicePackRepository = FakeVoicePackRepository(
            mutableListOf(
                TestVoicePackFactory.voicePack(
                    id = "native-us",
                    name = "美式原生离线包",
                    locale = "en-US",
                    accent = "us",
                    engineType = "sherpa_onnx",
                    status = VoicePackStatus.READY.storageValue,
                    isActive = true,
                ),
            ),
        )
        val viewModel = PronunciationSettingsViewModel(
            settingsRepository = settingsRepository,
            wordAudioRepository = FakeWordAudioRepository(),
            voicePackRepository = voicePackRepository,
            voicePackDownloadController = FakeVoicePackDownloadController(),
            pronunciationSourceRepository = sourceRepository,
            pronunciationSourceRegistry = PronunciationSourceRegistry(
                sourceRepository = sourceRepository,
                voicePackRepository = voicePackRepository,
                settingsRepository = settingsRepository,
            ),
        )

        val state = viewModel.setDefaultWordSource("native-us")

        assertEquals("美式原生离线包", state.defaultWordSourceLabel)
        assertTrue(state.sourceItems.single { it.id == "native-us" }.isDefaultForWord)
        assertFalse(state.sourceItems.single { it.id == "dictionary-uk" }.isDefaultForWord)
    }

    @Test
    fun loadUiStateMapsReadyVoicePackAndCacheSummary() = runTest {
        val settingsRepository = FakeSettingsRepository(
            initial = AppSettings(
                preferredPronunciationAccent = "us",
                allowCellularVoicePackDownload = true,
            ),
        )
        val voicePackRepository = FakeVoicePackRepository(
            mutableListOf(
                TestVoicePackFactory.voicePack(
                    id = "en-us-bridge-basic",
                    name = "美式基础桥接包",
                    locale = "en-US",
                    engineType = "system_tts_bridge",
                    status = VoicePackStatus.READY.storageValue,
                    isActive = true,
                ),
            ),
        )
        val viewModel = PronunciationSettingsViewModel(
            settingsRepository = settingsRepository,
            wordAudioRepository = FakeWordAudioRepository(cacheSizeBytes = 5L * 1024L * 1024L),
            voicePackRepository = voicePackRepository,
            voicePackDownloadController = FakeVoicePackDownloadController(),
            pronunciationSourceRepository = FakePronunciationSourceRepository(),
        )

        val state = viewModel.loadUiState()

        assertEquals("us", state.preferredAccent)
        assertTrue(state.audioCacheSummary.contains("5.00 MB"))
        assertEquals(1, state.voicePacks.size)
        assertEquals("系统语音桥接", state.voicePacks.single().engineLabel)
        assertFalse(state.voicePacks.single().canDownload)
        assertFalse(state.voicePacks.single().canActivate)
        assertTrue(state.voicePacks.single().canDelete)
    }

    @Test
    fun downloadVoicePackUsesAllowCellularSetting() = runTest {
        val settingsRepository = FakeSettingsRepository(
            initial = AppSettings(
                allowCellularVoicePackDownload = false,
            ),
        )
        val controller = FakeVoicePackDownloadController()
        val viewModel = PronunciationSettingsViewModel(
            settingsRepository = settingsRepository,
            wordAudioRepository = FakeWordAudioRepository(),
            voicePackRepository = FakeVoicePackRepository(
                mutableListOf(
                    TestVoicePackFactory.voicePack(id = "en-gb-bridge-basic"),
                ),
            ),
            voicePackDownloadController = controller,
            pronunciationSourceRepository = FakePronunciationSourceRepository(),
        )

        viewModel.downloadVoicePack("en-gb-bridge-basic")

        assertEquals(listOf("en-gb-bridge-basic" to false), controller.calls)
    }

    @Test
    fun loadUiStateMapsNativeVoicePackCapabilitiesAndHints() = runTest {
        val viewModel = PronunciationSettingsViewModel(
            settingsRepository = FakeSettingsRepository(initial = AppSettings()),
            wordAudioRepository = FakeWordAudioRepository(),
            voicePackRepository = FakeVoicePackRepository(
                mutableListOf(
                    TestVoicePackFactory.voicePack(
                        id = "en-us-offline-word-v1",
                        name = "美式离线发音包",
                        locale = "en-US",
                        accent = "us",
                        engineType = "sherpa_onnx",
                        status = VoicePackStatus.READY.storageValue,
                        isActive = true,
                        engineFamily = "native_neural_tts",
                        modelFamily = "kokoro",
                        supportsImportedWords = true,
                        estimatedStorageBytes = 512L * 1024L * 1024L,
                        estimatedRamMb = 768,
                        licenses = listOf("Apache-2.0"),
                    ),
                ),
            ),
            voicePackDownloadController = FakeVoicePackDownloadController(),
            pronunciationSourceRepository = FakePronunciationSourceRepository(),
        )

        val state = viewModel.loadUiState()
        val item = state.voicePacks.single()

        assertEquals("原生离线发音", item.engineLabel)
        assertTrue(item.capabilitySummary.contains("Excel 导入词书"))
        assertTrue(item.resourceHint.contains("512.00 MB"))
        assertTrue(item.resourceHint.contains("768 MB RAM"))
    }

    @Test
    fun loadUiStateWarnsWhenNativeVoicePackIsDistributionScaffold() = runTest {
        val viewModel = PronunciationSettingsViewModel(
            settingsRepository = FakeSettingsRepository(initial = AppSettings()),
            wordAudioRepository = FakeWordAudioRepository(),
            voicePackRepository = FakeVoicePackRepository(
                mutableListOf(
                    TestVoicePackFactory.voicePack(
                        id = "en-gb-offline-word-v1",
                        name = "英式离线发音包",
                        locale = "en-GB",
                        accent = "uk",
                        engineType = "sherpa_onnx",
                        status = VoicePackStatus.NOT_INSTALLED.storageValue,
                        modelFamily = "sherpa_onnx_scaffold",
                        supportsImportedWords = true,
                    ),
                ),
            ),
            voicePackDownloadController = FakeVoicePackDownloadController(),
            pronunciationSourceRepository = FakePronunciationSourceRepository(),
        )

        val state = viewModel.loadUiState()
        val item = state.voicePacks.single()

        assertTrue(item.capabilitySummary.contains("分发链路"))
        assertTrue(item.capabilitySummary.contains("模型执行桥接"))
    }

    @Test
    fun loadUiStateExposesBrokenVoicePackFailureReason() = runTest {
        val controller = FakeVoicePackDownloadController(
            failureMessages = mapOf(
                "en-us-offline-word-v1" to "原生语音包 manifest 缺少 entryFiles 声明。",
            ),
        )
        val viewModel = PronunciationSettingsViewModel(
            settingsRepository = FakeSettingsRepository(initial = AppSettings()),
            wordAudioRepository = FakeWordAudioRepository(),
            voicePackRepository = FakeVoicePackRepository(
                mutableListOf(
                    TestVoicePackFactory.voicePack(
                        id = "en-us-offline-word-v1",
                        name = "美式离线发音包",
                        engineType = "sherpa_onnx",
                        status = VoicePackStatus.BROKEN.storageValue,
                    ),
                ),
            ),
            voicePackDownloadController = controller,
            pronunciationSourceRepository = FakePronunciationSourceRepository(),
        )

        val state = viewModel.loadUiState()
        val item = state.voicePacks.single()

        assertEquals("安装失败", item.statusLabel)
        assertEquals("原生语音包 manifest 缺少 entryFiles 声明。", item.failureReason)
    }

    @Test
    fun loadUiState_mapsDownloadVerifyInstallRuntimeFailuresToDifferentMessages() = runTest {
        suspend fun stateFor(reason: String): VoicePackItemUiState {
            val viewModel = PronunciationSettingsViewModel(
                settingsRepository = FakeSettingsRepository(initial = AppSettings()),
                wordAudioRepository = FakeWordAudioRepository(),
                voicePackRepository = FakeVoicePackRepository(
                    mutableListOf(
                        TestVoicePackFactory.voicePack(
                            id = "en-us-offline-word-v1",
                            name = "美式离线发音包",
                            engineType = "sherpa_onnx",
                            status = VoicePackStatus.BROKEN.storageValue,
                        ),
                    ),
                ),
                voicePackDownloadController = FakeVoicePackDownloadController(
                    failureMessages = mapOf("en-us-offline-word-v1" to reason),
                ),
                pronunciationSourceRepository = FakePronunciationSourceRepository(),
            )
            return viewModel.loadUiState().voicePacks.single()
        }

        assertEquals("下载失败", stateFor("download_failed").statusLabel)
        assertEquals("校验失败", stateFor("payload checksum mismatch").statusLabel)
        assertEquals("安装失败", stateFor("manifest 缺少 entryFiles").statusLabel)
        assertEquals("运行异常", stateFor("runtime_failed").statusLabel)
    }

    @Test
    fun activateVoicePack_keepsOtherAccentActive() = runTest {
        val viewModel = PronunciationSettingsViewModel(
            settingsRepository = FakeSettingsRepository(initial = AppSettings()),
            wordAudioRepository = FakeWordAudioRepository(),
            voicePackRepository = FakeVoicePackRepository(
                mutableListOf(
                    TestVoicePackFactory.voicePack(
                        id = "en-gb-offline-word-v1",
                        locale = "en-GB",
                        accent = "uk",
                        engineType = "sherpa_onnx",
                        status = VoicePackStatus.READY.storageValue,
                        isActive = true,
                    ),
                    TestVoicePackFactory.voicePack(
                        id = "en-us-offline-word-v1",
                        locale = "en-US",
                        accent = "us",
                        engineType = "sherpa_onnx",
                        status = VoicePackStatus.READY.storageValue,
                    ),
                ),
            ),
            voicePackDownloadController = FakeVoicePackDownloadController(),
            pronunciationSourceRepository = FakePronunciationSourceRepository(),
        )

        val state = viewModel.activateVoicePack("en-us-offline-word-v1")

        assertTrue(state.voicePacks.single { it.id == "en-gb-offline-word-v1" }.isActive)
        assertTrue(state.voicePacks.single { it.id == "en-us-offline-word-v1" }.isActive)
    }
}

private class FakeVoicePackDownloadController(
    private val failureMessages: Map<String, String> = emptyMap(),
) : VoicePackDownloadController {
    val calls = mutableListOf<Pair<String, Boolean>>()

    override suspend fun enqueue(voicePackId: String, allowCellular: Boolean) {
        calls += voicePackId to allowCellular
    }

    override suspend fun latestFailureMessage(voicePackId: String): String? =
        failureMessages[voicePackId]
}

private class FakeSettingsRepository(
    initial: AppSettings,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
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

    override suspend fun updatePreferredPronunciationAccent(accent: String) {
        state.value = state.value.copy(preferredPronunciationAccent = accent)
    }

    override suspend fun updatePronunciationMode(mode: String) = Unit

    override suspend fun updateAllowCellularVoicePackDownload(enabled: Boolean) {
        state.value = state.value.copy(allowCellularVoicePackDownload = enabled)
    }

    override suspend fun updateAutoCacheWordAudio(enabled: Boolean) = Unit

    override suspend fun updateAudioCacheLimitMb(limitMb: Int) = Unit

    override suspend fun updateActiveVoicePackId(voicePackId: String?) = Unit

    override suspend fun updateFallbackToSystemTts(enabled: Boolean) = Unit

    override suspend fun updatePreferOfflineForLongText(enabled: Boolean) = Unit

    override suspend fun updateReminderEnabled(enabled: Boolean) = Unit

    override suspend fun updateReminderTime(hour: Int, minute: Int) = Unit
}

private class FakePronunciationSourceRepository(
    private val sources: MutableList<PronunciationSource> = mutableListOf(),
) : PronunciationSourceRepository {
    override suspend fun getAllSources(): List<PronunciationSource> = sources.toList()

    override suspend fun getSource(sourceId: String): PronunciationSource? =
        sources.firstOrNull { it.id == sourceId }

    override suspend fun upsertSources(sources: List<PronunciationSource>) {
        sources.forEach { source ->
            val index = this.sources.indexOfFirst { it.id == source.id }
            if (index >= 0) {
                this.sources[index] = source
            } else {
                this.sources += source
            }
        }
    }

    override suspend fun setDefaultWordSource(sourceId: String) {
        if (sources.none { it.id == sourceId }) return
        sources.indices.forEach { index ->
            val source = sources[index]
            sources[index] = source.copy(isDefaultForWord = source.id == sourceId)
        }
    }

    override suspend fun setDefaultLongTextSource(sourceId: String) {
        if (sources.none { it.id == sourceId }) return
        sources.indices.forEach { index ->
            val source = sources[index]
            sources[index] = source.copy(isDefaultForLongText = source.id == sourceId)
        }
    }

    override suspend fun clearAll() {
        sources.clear()
    }
}

private class FakeWordAudioRepository(
    private val cacheSizeBytes: Long = 0L,
) : WordAudioRepository {
    override suspend fun findCachedAsset(
        wordId: Long,
        accent: PronunciationAccent,
    ): WordAudioAsset? = null

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

    override suspend fun markPlayed(asset: WordAudioAsset) = Unit

    override suspend fun clearDictionaryCache(): Int = 0

    override suspend fun cacheSizeBytes(): Long = cacheSizeBytes
}

private class FakeVoicePackRepository(
    private val packs: MutableList<com.yueliangmanle.danci.core.model.VoicePack>,
) : VoicePackRepository {
    override suspend fun getAllVoicePacks(): List<com.yueliangmanle.danci.core.model.VoicePack> = packs.toList()

    override suspend fun getVoicePack(id: String): com.yueliangmanle.danci.core.model.VoicePack? =
        packs.firstOrNull { it.id == id }

    override suspend fun getActiveVoicePack(): com.yueliangmanle.danci.core.model.VoicePack? =
        packs.filter { it.isActive }.singleOrNull()

    override suspend fun getActiveVoicePack(
        accent: PronunciationAccent,
    ): com.yueliangmanle.danci.core.model.VoicePack? =
        when (accent) {
            PronunciationAccent.AUTO -> getActiveVoicePack()
            else -> packs.firstOrNull { pack ->
                pack.isActive && PronunciationAccent.fromStorageValue(pack.accent) == accent
            }
        }

    override suspend fun activateVoicePack(id: String) {
        val index = packs.indexOfFirst { it.id == id }
        if (index < 0) return
        val accent = packs[index].accent
        for (itemIndex in packs.indices) {
            val pack = packs[itemIndex]
            packs[itemIndex] = pack.copy(
                isActive = when {
                    pack.id == id -> true
                    pack.accent == accent -> false
                    else -> pack.isActive
                },
            )
        }
    }

    override suspend fun upsertVoicePack(voicePack: com.yueliangmanle.danci.core.model.VoicePack) {
        val index = packs.indexOfFirst { it.id == voicePack.id }
        if (index >= 0) {
            packs[index] = voicePack
        } else {
            packs += voicePack
        }
    }

    override suspend fun removeVoicePack(id: String) {
        packs.removeAll { it.id == id }
    }

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

private fun testSource(
    id: String,
    type: PronunciationSourceType,
    accent: PronunciationAccent,
    isDefaultForWord: Boolean = false,
    isDefaultForLongText: Boolean = false,
    backingVoicePackId: String? = null,
): PronunciationSource =
    PronunciationSource(
        id = id,
        name = when (type) {
            PronunciationSourceType.DICTIONARY -> "词典发音（${accent.label}）"
            PronunciationSourceType.LOCAL_NATIVE -> "美式原生离线包"
            PronunciationSourceType.LOCAL_BRIDGE -> "桥接语音包"
            PronunciationSourceType.CLOUD_TTS -> "云端 TTS"
        },
        sourceType = type.storageValue,
        accent = accent.storageValue,
        enabled = true,
        isDefaultForWord = isDefaultForWord,
        isDefaultForLongText = isDefaultForLongText,
        backingVoicePackId = backingVoicePackId,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
