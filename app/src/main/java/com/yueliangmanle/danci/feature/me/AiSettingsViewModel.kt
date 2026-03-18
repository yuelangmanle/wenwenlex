package com.yueliangmanle.danci.feature.me

import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.security.AiCredentialStore

data class AiSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEnabled: Boolean = false,
    val baseUrl: String = "",
    val model: String = "",
    val apiKeyInput: String = "",
    val hasApiKey: Boolean = false,
    val enablePlanAdjustments: Boolean = true,
    val enableSessionCheckpoints: Boolean = true,
    val statusMessage: String? = null,
)

class AiSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val credentialStore: AiCredentialStore,
) {
    suspend fun loadUiState(): AiSettingsUiState {
        val settings = settingsRepository.getSettings()
        val apiKey = credentialStore.readApiKey()
        return AiSettingsUiState(
            isEnabled = settings.aiEnabled,
            baseUrl = settings.aiBaseUrl,
            model = settings.aiModel,
            apiKeyInput = "",
            hasApiKey = !apiKey.isNullOrBlank(),
            enablePlanAdjustments = settings.aiPlanAdjustmentEnabled,
            enableSessionCheckpoints = settings.aiSessionCheckpointEnabled,
        )
    }

    suspend fun save(state: AiSettingsUiState): AiSettingsUiState {
        settingsRepository.updateAiEnabled(state.isEnabled)
        settingsRepository.updateAiBaseUrl(state.baseUrl.trim())
        settingsRepository.updateAiModel(state.model.trim())
        settingsRepository.updateAiPlanAdjustmentEnabled(state.enablePlanAdjustments)
        settingsRepository.updateAiSessionCheckpointEnabled(state.enableSessionCheckpoints)
        if (state.apiKeyInput.isBlank()) {
            if (!state.hasApiKey) {
                credentialStore.clearApiKey()
            }
        } else {
            credentialStore.saveApiKey(state.apiKeyInput.trim())
        }
        return loadUiState().copy(
            statusMessage = if (state.isEnabled) {
                "AI 配置已保存，可以在首页手动分析计划。"
            } else {
                "AI 已关闭，核心学习功能仍可离线使用。"
            },
        )
    }
}
