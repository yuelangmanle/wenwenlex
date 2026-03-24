package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.WordAudioAsset
import com.yueliangmanle.danci.core.security.AiCredentialStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudTtsEngineTest {
    @Test
    fun healthChecker_returnsHealthyWhenProviderResponds200() = runTest {
        val checker = CloudTtsApiHealthChecker(
            registry = CloudTtsProviderRegistry(
                providers = listOf(
                    FakeCloudTtsProvider(
                        health = CloudTtsHealthResult(
                            healthy = true,
                            message = "MiMo 接口响应正常。",
                        ),
                    ),
                ),
            ),
        )

        val result = checker.check(
            CloudTtsProviderConfig(
                providerId = "mimo",
                baseUrl = "https://api.xiaomimimo.com/v1",
                model = "mimo-v2-tts",
                apiKey = "secret",
                presetId = "default_en",
            ),
        )

        assertTrue(result.healthy)
        assertEquals("MiMo 接口响应正常。", result.message)
    }

    @Test
    fun engine_cachesSynthesizedAudioIntoCloudBucket() = runTest {
        val repository = FakeCloudWordAudioRepository()
        val engine = CloudTtsEngine(
            settingsRepository = FakeCloudSettingsRepository(
                AppSettings(
                    defaultCloudTtsProviderId = "mimo",
                    defaultCloudTtsPresetId = "default_en",
                    cloudTtsBaseUrl = "https://api.xiaomimimo.com/v1",
                    cloudTtsModel = "mimo-v2-tts",
                ),
            ),
            credentialStore = FakeAiCredentialStore("secret"),
            wordAudioRepository = repository,
            registry = CloudTtsProviderRegistry(
                providers = listOf(
                    FakeCloudTtsProvider(
                        result = CloudTtsSynthesisResult(
                            audioBytes = byteArrayOf(0x01, 0x02, 0x03),
                            mimeType = "audio/wav",
                            providerLabel = "MiMo",
                            presetLabel = "英文女声",
                        ),
                    ),
                ),
            ),
        )

        val asset = engine.synthesizeWord(
            word = Word(id = 7L, lemma = "abandon"),
            accent = PronunciationAccent.US,
        )

        assertEquals(7L, asset?.wordId)
        assertEquals("online_prebuilt_cache", asset?.sourceType)
        assertEquals(1, repository.cachedAssets.size)
    }
}

private class FakeCloudSettingsRepository(
    initial: AppSettings,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

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
    override suspend fun updateDefaultCloudTtsProviderId(providerId: String?) {
        state.value = state.value.copy(defaultCloudTtsProviderId = providerId)
    }
    override suspend fun updateDefaultCloudTtsPresetId(presetId: String?) {
        state.value = state.value.copy(defaultCloudTtsPresetId = presetId)
    }
    override suspend fun updateCloudTtsBaseUrl(baseUrl: String) {
        state.value = state.value.copy(cloudTtsBaseUrl = baseUrl)
    }
    override suspend fun updateCloudTtsModel(model: String) {
        state.value = state.value.copy(cloudTtsModel = model)
    }
}

private class FakeAiCredentialStore(
    private val apiKey: String?,
) : AiCredentialStore {
    override suspend fun saveApiKey(key: String) = Unit
    override suspend fun readApiKey(): String? = apiKey
    override suspend fun clearApiKey() = Unit
    override suspend fun saveApiKey(profileId: String, key: String) = Unit
    override suspend fun readApiKey(profileId: String): String? = apiKey
    override suspend fun clearApiKey(profileId: String) = Unit
}

private class FakeCloudWordAudioRepository : WordAudioRepository {
    val cachedAssets = mutableListOf<WordAudioAsset>()

    override suspend fun findCachedAsset(wordId: Long, accent: PronunciationAccent): WordAudioAsset? = null
    override suspend fun isRemoteLookupCoolingDown(wordId: Long, accent: PronunciationAccent): Boolean = false
    override suspend fun cacheDictionaryAudio(wordId: Long, candidate: DictionaryAudioCandidate): WordAudioAsset? = null
    override suspend fun markRemoteLookupFailure(wordId: Long, accent: PronunciationAccent, errorMessage: String) = Unit
    override suspend fun markPlayed(asset: WordAudioAsset) = Unit
    override suspend fun clearDictionaryCache(): Int = 0
    override suspend fun cacheSizeBytes(): Long = 0L
    override suspend fun findCloudTtsAsset(wordId: Long, accent: PronunciationAccent): WordAudioAsset? = null
    override suspend fun cacheCloudTtsAudio(
        wordId: Long,
        accent: PronunciationAccent,
        normalizedWord: String,
        audioBytes: ByteArray,
        mimeType: String,
    ): WordAudioAsset {
        return WordAudioAsset(
            id = cachedAssets.size.toLong() + 1L,
            wordId = wordId,
            accent = accent.storageValue,
            sourceType = "online_prebuilt_cache",
            localPath = "/tmp/$normalizedWord.wav",
            mimeType = mimeType,
            status = "ready",
        ).also { cachedAssets += it }
    }
}

private class FakeCloudTtsProvider(
    private val health: CloudTtsHealthResult = CloudTtsHealthResult(false, "not set"),
    private val result: CloudTtsSynthesisResult? = null,
) : CloudTtsProvider {
    override val providerId: String = "mimo"
    override val providerLabel: String = "MiMo"

    override fun presets(): List<CloudTtsPreset> =
        listOf(
            CloudTtsPreset(
                id = "default_en",
                label = "英文女声",
                voice = "default_en",
            ),
        )

    override suspend fun synthesize(
        text: String,
        accent: PronunciationAccent,
        config: CloudTtsProviderConfig,
    ): CloudTtsSynthesisResult? = result

    override suspend fun checkHealth(config: CloudTtsProviderConfig): CloudTtsHealthResult = health
}
