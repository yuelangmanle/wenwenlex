package com.yueliangmanle.danci.feature.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.DEFAULT_CLOUD_TTS_BASE_URL
import com.yueliangmanle.danci.core.data.DEFAULT_CLOUD_TTS_MODEL
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.pronunciation.CloudTtsApiHealthChecker
import com.yueliangmanle.danci.core.pronunciation.CloudTtsProviderConfig
import com.yueliangmanle.danci.core.pronunciation.CloudTtsProviderRegistry
import com.yueliangmanle.danci.core.pronunciation.cloudTtsCredentialProfileId
import com.yueliangmanle.danci.core.security.AiCredentialStore
import com.yueliangmanle.danci.core.security.buildAiCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CloudTtsProviderItemUiState(
    val id: String,
    val label: String,
    val isSelected: Boolean,
)

data class CloudTtsPresetItemUiState(
    val id: String,
    val label: String,
    val voice: String,
    val isSelected: Boolean,
)

data class CloudTtsSettingsUiState(
    val isLoading: Boolean = false,
    val defaultProviderId: String? = null,
    val selectedPresetId: String? = null,
    val baseUrl: String = DEFAULT_CLOUD_TTS_BASE_URL,
    val model: String = DEFAULT_CLOUD_TTS_MODEL,
    val apiKeyInput: String = "",
    val hasSavedApiKey: Boolean = false,
    val providers: List<CloudTtsProviderItemUiState> = emptyList(),
    val presets: List<CloudTtsPresetItemUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class CloudTtsSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val credentialStore: AiCredentialStore,
    private val registry: CloudTtsProviderRegistry,
    private val healthChecker: CloudTtsApiHealthChecker,
) {
    suspend fun loadUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): CloudTtsSettingsUiState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val provider = registry.findProvider(settings.defaultCloudTtsProviderId)
        val defaultProviderId = settings.defaultCloudTtsProviderId
            ?.takeIf(String::isNotBlank)
            ?: provider?.providerId
        val presets = provider?.presets().orEmpty()
        val selectedPresetId = settings.defaultCloudTtsPresetId
            ?.takeIf { presetId -> presets.any { it.id == presetId } }
            ?: presets.firstOrNull()?.id
        val hasSavedApiKey = defaultProviderId
            ?.let(::cloudTtsCredentialProfileId)
            ?.let { profileId -> !credentialStore.readApiKey(profileId).isNullOrBlank() }
            ?: false
        CloudTtsSettingsUiState(
            defaultProviderId = defaultProviderId,
            selectedPresetId = selectedPresetId,
            baseUrl = settings.cloudTtsBaseUrl,
            model = settings.cloudTtsModel,
            hasSavedApiKey = hasSavedApiKey,
            providers = registry.allProviders().map { item ->
                CloudTtsProviderItemUiState(
                    id = item.providerId,
                    label = item.providerLabel,
                    isSelected = item.providerId == defaultProviderId,
                )
            },
            presets = presets.map { preset ->
                CloudTtsPresetItemUiState(
                    id = preset.id,
                    label = preset.label,
                    voice = preset.voice,
                    isSelected = preset.id == selectedPresetId,
                )
            },
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun selectProvider(providerId: String): CloudTtsSettingsUiState {
        settingsRepository.updateDefaultCloudTtsProviderId(providerId)
        val firstPresetId = registry.findProvider(providerId)?.presets()?.firstOrNull()?.id
        settingsRepository.updateDefaultCloudTtsPresetId(firstPresetId)
        return loadUiState(statusMessage = "云端 TTS provider 已更新。")
    }

    suspend fun selectPreset(presetId: String): CloudTtsSettingsUiState {
        settingsRepository.updateDefaultCloudTtsPresetId(presetId)
        return loadUiState(statusMessage = "云端 TTS 预设已更新。")
    }

    suspend fun updateBaseUrl(baseUrl: String): CloudTtsSettingsUiState {
        settingsRepository.updateCloudTtsBaseUrl(baseUrl)
        return loadUiState(statusMessage = "云端 TTS Base URL 已保存。")
    }

    suspend fun updateModel(model: String): CloudTtsSettingsUiState {
        settingsRepository.updateCloudTtsModel(model)
        return loadUiState(statusMessage = "云端 TTS 模型已保存。")
    }

    suspend fun saveApiKey(
        providerId: String,
        apiKey: String,
    ): CloudTtsSettingsUiState {
        require(providerId.isNotBlank()) { "请先选择云端 TTS provider。" }
        require(apiKey.isNotBlank()) { "请输入 API Key。" }
        credentialStore.saveApiKey(cloudTtsCredentialProfileId(providerId), apiKey.trim())
        return loadUiState(statusMessage = "云端 TTS API Key 已保存到本机安全存储。")
    }

    suspend fun clearApiKey(providerId: String): CloudTtsSettingsUiState {
        require(providerId.isNotBlank()) { "请先选择云端 TTS provider。" }
        credentialStore.clearApiKey(cloudTtsCredentialProfileId(providerId))
        return loadUiState(statusMessage = "云端 TTS API Key 已清除。")
    }

    suspend fun checkHealth(current: CloudTtsSettingsUiState): CloudTtsSettingsUiState {
        val providerId = current.defaultProviderId
            ?: return current.copy(errorMessage = "请先选择云端 TTS provider。")
        settingsRepository.updateCloudTtsBaseUrl(current.baseUrl)
        settingsRepository.updateCloudTtsModel(current.model)
        val apiKey = current.apiKeyInput.trim().ifBlank {
            credentialStore.readApiKey(cloudTtsCredentialProfileId(providerId)).orEmpty()
        }
        if (apiKey.isBlank()) {
            return current.copy(errorMessage = "请先输入并保存 API Key。")
        }
        val config = CloudTtsProviderConfig(
            providerId = providerId,
            baseUrl = current.baseUrl.ifBlank { DEFAULT_CLOUD_TTS_BASE_URL },
            model = current.model.ifBlank { DEFAULT_CLOUD_TTS_MODEL },
            apiKey = apiKey,
            presetId = current.selectedPresetId.orEmpty(),
        )
        val result = healthChecker.check(config)
        return loadUiState(
            statusMessage = result.message.takeIf { result.healthy },
            errorMessage = result.message.takeUnless { result.healthy },
        )
    }
}

suspend fun loadCloudTtsSettingsViewModel(context: Context): CloudTtsSettingsViewModel =
    withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val registry = CloudTtsProviderRegistry(
            providers = listOf(com.yueliangmanle.danci.core.pronunciation.MiMoCloudTtsProvider()),
        )
        CloudTtsSettingsViewModel(
            settingsRepository = buildSettingsRepository(appContext),
            credentialStore = buildAiCredentialStore(appContext),
            registry = registry,
            healthChecker = CloudTtsApiHealthChecker(registry),
        )
    }
