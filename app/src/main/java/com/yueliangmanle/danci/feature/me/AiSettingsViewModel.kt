package com.yueliangmanle.danci.feature.me

import com.yueliangmanle.danci.core.ai.ensureLegacyAiProfileMigrated
import com.yueliangmanle.danci.core.data.AiProfileRepository
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.DEFAULT_AI_BASE_URL
import com.yueliangmanle.danci.core.data.DEFAULT_AI_MODEL
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.security.AiCredentialStore
import java.time.Instant
import java.util.UUID

data class AiProfileListItemUiState(
    val id: String,
    val name: String,
    val providerType: String,
    val baseUrl: String,
    val model: String,
    val enabled: Boolean,
    val hasApiKey: Boolean,
    val badges: List<String> = emptyList(),
)

data class AiProfileEditorState(
    val id: String? = null,
    val name: String = "",
    val providerType: String = "custom",
    val baseUrl: String = DEFAULT_AI_BASE_URL,
    val model: String = DEFAULT_AI_MODEL,
    val enabled: Boolean = true,
    val apiKeyInput: String = "",
    val hasSavedApiKey: Boolean = false,
)

data class AiSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEnabled: Boolean = false,
    val enablePlanAdjustments: Boolean = true,
    val enableSessionCheckpoints: Boolean = true,
    val profiles: List<AiProfileListItemUiState> = emptyList(),
    val editor: AiProfileEditorState = AiProfileEditorState(),
    val defaultAiProfileId: String? = null,
    val wordHelpProfileId: String? = null,
    val planAdjustmentProfileId: String? = null,
    val phoneticFillProfileId: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class AiSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val aiProfileRepository: AiProfileRepository,
    private val credentialStore: AiCredentialStore,
) {
    suspend fun loadUiState(
        selectedProfileId: String? = null,
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): AiSettingsUiState {
        ensureLegacyAiProfileMigrated(settingsRepository, aiProfileRepository, credentialStore)
        val settings = settingsRepository.getSettings()
        val profiles = aiProfileRepository.getProfiles()
        val keyStates = profiles.associate { profile ->
            profile.id to !credentialStore.readApiKey(profile.id).isNullOrBlank()
        }
        val targetProfile = profiles.firstOrNull { it.id == selectedProfileId }
            ?: profiles.firstOrNull { it.id == settings.defaultAiProfileId }
            ?: profiles.firstOrNull()
        return AiSettingsUiState(
            isEnabled = settings.aiEnabled,
            enablePlanAdjustments = settings.aiPlanAdjustmentEnabled,
            enableSessionCheckpoints = settings.aiSessionCheckpointEnabled,
            profiles = profiles.map { profile ->
                profile.asListItem(
                    settings = settings,
                    hasApiKey = keyStates[profile.id] == true,
                )
            },
            editor = targetProfile?.asEditor(
                hasSavedApiKey = keyStates[targetProfile.id] == true,
            ) ?: newProfileEditor(),
            defaultAiProfileId = settings.defaultAiProfileId,
            wordHelpProfileId = settings.wordHelpProfileId,
            planAdjustmentProfileId = settings.planAdjustmentProfileId,
            phoneticFillProfileId = settings.phoneticFillProfileId,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    fun startNewProfile(current: AiSettingsUiState): AiSettingsUiState =
        current.copy(
            editor = newProfileEditor(),
            statusMessage = null,
            errorMessage = null,
        )

    suspend fun saveProfile(current: AiSettingsUiState): AiSettingsUiState {
        val editor = current.editor
        require(editor.name.isNotBlank()) { "请先填写档案名称。" }
        require(editor.baseUrl.isNotBlank()) { "请先填写 Base URL。" }
        require(editor.model.isNotBlank()) { "请先填写模型名称。" }
        val existing = if (editor.id != null) {
            aiProfileRepository.getProfile(editor.id)
        } else {
            null
        }
        val profileId = existing?.id ?: "profile-${UUID.randomUUID().toString().take(8)}"
        val now = Instant.now()
        aiProfileRepository.saveProfile(
            AiProviderProfile(
                id = profileId,
                name = editor.name.trim(),
                providerType = editor.providerType,
                baseUrl = editor.baseUrl.trim(),
                model = editor.model.trim(),
                enabled = editor.enabled,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                lastValidatedAt = existing?.lastValidatedAt,
            ),
        )
        if (editor.apiKeyInput.isNotBlank()) {
            credentialStore.saveApiKey(profileId, editor.apiKeyInput.trim())
        }
        if (current.defaultAiProfileId.isNullOrBlank()) {
            settingsRepository.updateDefaultAiProfileId(profileId)
        }
        return loadUiState(
            selectedProfileId = profileId,
            statusMessage = "API 档案已保存。",
        )
    }

    suspend fun clearSelectedApiKey(current: AiSettingsUiState): AiSettingsUiState {
        val profileId = current.editor.id ?: return current.copy(errorMessage = "新建档案还没有已保存密钥。")
        credentialStore.clearApiKey(profileId)
        return loadUiState(
            selectedProfileId = profileId,
            statusMessage = "已清空这条档案的本地密钥。",
        )
    }

    suspend fun deleteSelectedProfile(current: AiSettingsUiState): AiSettingsUiState {
        val profileId = current.editor.id ?: return current.copy(errorMessage = "当前没有可删除的已保存档案。")
        aiProfileRepository.deleteProfile(profileId)
        credentialStore.clearApiKey(profileId)

        val settings = settingsRepository.getSettings()
        val remainingProfiles = aiProfileRepository.getProfiles()
        val fallbackDefault = if (settings.defaultAiProfileId == profileId) {
            remainingProfiles.firstOrNull()?.id
        } else {
            settings.defaultAiProfileId
        }
        settingsRepository.updateDefaultAiProfileId(fallbackDefault)
        if (settings.wordHelpProfileId == profileId) {
            settingsRepository.updateWordHelpProfileId(null)
        }
        if (settings.planAdjustmentProfileId == profileId) {
            settingsRepository.updatePlanAdjustmentProfileId(null)
        }
        if (settings.phoneticFillProfileId == profileId) {
            settingsRepository.updatePhoneticFillProfileId(null)
        }

        return loadUiState(
            selectedProfileId = remainingProfiles.firstOrNull()?.id,
            statusMessage = "API 档案已删除。",
        )
    }

    suspend fun saveGlobalSettings(current: AiSettingsUiState): AiSettingsUiState {
        settingsRepository.updateAiEnabled(current.isEnabled)
        settingsRepository.updateAiPlanAdjustmentEnabled(current.enablePlanAdjustments)
        settingsRepository.updateAiSessionCheckpointEnabled(current.enableSessionCheckpoints)
        settingsRepository.updateDefaultAiProfileId(current.defaultAiProfileId)
        settingsRepository.updateWordHelpProfileId(current.wordHelpProfileId)
        settingsRepository.updatePlanAdjustmentProfileId(current.planAdjustmentProfileId)
        settingsRepository.updatePhoneticFillProfileId(current.phoneticFillProfileId)
        return loadUiState(
            selectedProfileId = current.editor.id,
            statusMessage = "全局 AI 路由已保存。",
        )
    }

    private fun newProfileEditor(): AiProfileEditorState =
        AiProfileEditorState(
            name = "",
            providerType = "custom",
            baseUrl = DEFAULT_AI_BASE_URL,
            model = DEFAULT_AI_MODEL,
            enabled = true,
            apiKeyInput = "",
            hasSavedApiKey = false,
        )
}

private fun AiProviderProfile.asListItem(
    settings: AppSettings,
    hasApiKey: Boolean,
): AiProfileListItemUiState =
    AiProfileListItemUiState(
        id = id,
        name = name,
        providerType = providerType,
        baseUrl = baseUrl,
        model = model,
        enabled = enabled,
        hasApiKey = hasApiKey,
        badges = buildList {
            if (settings.defaultAiProfileId == id) add("默认")
            if (settings.wordHelpProfileId == id) add("助记/辨析")
            if (settings.planAdjustmentProfileId == id) add("计划调整")
            if (settings.phoneticFillProfileId == id) add("音标补全")
            if (hasApiKey) add("已存密钥")
            if (!enabled) add("已禁用")
        },
    )

private fun AiProviderProfile.asEditor(
    hasSavedApiKey: Boolean,
): AiProfileEditorState =
    AiProfileEditorState(
        id = id,
        name = name,
        providerType = providerType,
        baseUrl = baseUrl,
        model = model,
        enabled = enabled,
        apiKeyInput = "",
        hasSavedApiKey = hasSavedApiKey,
    )
