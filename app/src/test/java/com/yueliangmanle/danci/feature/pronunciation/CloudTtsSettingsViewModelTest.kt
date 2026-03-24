package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.security.AiCredentialStore
import com.yueliangmanle.danci.core.pronunciation.CloudTtsApiHealthChecker
import com.yueliangmanle.danci.core.pronunciation.CloudTtsProviderConfig
import com.yueliangmanle.danci.core.pronunciation.CloudTtsProviderRegistry
import com.yueliangmanle.danci.core.pronunciation.CloudTtsHealthResult
import com.yueliangmanle.danci.core.pronunciation.CloudTtsPreset
import com.yueliangmanle.danci.core.pronunciation.CloudTtsProvider
import com.yueliangmanle.danci.core.model.PronunciationAccent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudTtsSettingsViewModelTest {
    @Test
    fun settingsViewModel_updatesDefaultProviderAndPreset() = runTest {
        val settingsRepository = FakeCloudTtsSettingsRepository(
            AppSettings(
                defaultCloudTtsProviderId = null,
                defaultCloudTtsPresetId = null,
            ),
        )
        val viewModel = CloudTtsSettingsViewModel(
            settingsRepository = settingsRepository,
            credentialStore = FakeCloudTtsCredentialStore(),
            registry = CloudTtsProviderRegistry(
                providers = listOf(FakeCloudTtsSettingsProvider()),
            ),
            healthChecker = CloudTtsApiHealthChecker(
                registry = CloudTtsProviderRegistry(
                    providers = listOf(FakeCloudTtsSettingsProvider()),
                ),
            ),
        )

        viewModel.selectProvider("mimo")
        viewModel.selectPreset("default_en")
        val state = viewModel.loadUiState()

        assertEquals("mimo", state.defaultProviderId)
        assertEquals("default_en", state.selectedPresetId)
    }
}

private class FakeCloudTtsSettingsRepository(
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

private class FakeCloudTtsCredentialStore : AiCredentialStore {
    override suspend fun saveApiKey(key: String) = Unit
    override suspend fun readApiKey(): String? = null
    override suspend fun clearApiKey() = Unit
    override suspend fun saveApiKey(profileId: String, key: String) = Unit
    override suspend fun readApiKey(profileId: String): String? = null
    override suspend fun clearApiKey(profileId: String) = Unit
}

private class FakeCloudTtsSettingsProvider : CloudTtsProvider {
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
    ) = null

    override suspend fun checkHealth(config: CloudTtsProviderConfig): CloudTtsHealthResult =
        CloudTtsHealthResult(true, "ok")
}
