package com.yueliangmanle.danci.feature.me

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.data.buildAiProfileRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.security.buildAiCredentialStore
import kotlinx.coroutines.launch

@Composable
fun AiSettingsRoute() {
    val context = LocalContext.current
    val viewModel = remember(context) {
        AiSettingsViewModel(
            settingsRepository = buildSettingsRepository(context),
            aiProfileRepository = buildAiProfileRepository(context),
            credentialStore = buildAiCredentialStore(context),
        )
    }
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf(AiSettingsUiState(isLoading = true))
    }

    LaunchedEffect(viewModel) {
        state = viewModel.loadUiState()
    }

    fun launchSaving(action: suspend () -> AiSettingsUiState) {
        scope.launch {
            state = state.copy(isSaving = true, statusMessage = null, errorMessage = null)
            state = runCatching { action() }.getOrElse { error ->
                state.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "保存失败，请稍后重试。",
                )
            }.copy(isSaving = false)
        }
    }

    AiSettingsScreen(
        state = state,
        onEnabledChange = { state = state.copy(isEnabled = it) },
        onPlanAdjustmentsChange = { state = state.copy(enablePlanAdjustments = it) },
        onSessionCheckpointsChange = { state = state.copy(enableSessionCheckpoints = it) },
        onDefaultProfileChange = { state = state.copy(defaultAiProfileId = it) },
        onWordHelpProfileChange = { state = state.copy(wordHelpProfileId = it) },
        onPlanAdjustmentProfileChange = { state = state.copy(planAdjustmentProfileId = it) },
        onPhoneticFillProfileChange = { state = state.copy(phoneticFillProfileId = it) },
        onSelectProfile = { profileId ->
            scope.launch {
                state = state.copy(isLoading = true)
                state = viewModel.loadUiState(selectedProfileId = profileId)
            }
        },
        onNewProfileClick = {
            state = viewModel.startNewProfile(state)
        },
        onEditorNameChange = { state = state.copy(editor = state.editor.copy(name = it)) },
        onEditorBaseUrlChange = { state = state.copy(editor = state.editor.copy(baseUrl = it)) },
        onEditorModelChange = { state = state.copy(editor = state.editor.copy(model = it)) },
        onEditorApiKeyChange = { state = state.copy(editor = state.editor.copy(apiKeyInput = it)) },
        onEditorEnabledChange = { state = state.copy(editor = state.editor.copy(enabled = it)) },
        onSaveProfileClick = {
            launchSaving { viewModel.saveProfile(state) }
        },
        onClearApiKeyClick = {
            launchSaving { viewModel.clearSelectedApiKey(state) }
        },
        onDeleteProfileClick = {
            launchSaving { viewModel.deleteSelectedProfile(state) }
        },
        onSaveGlobalClick = {
            launchSaving { viewModel.saveGlobalSettings(state) }
        },
    )
}
