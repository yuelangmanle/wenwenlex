package com.yueliangmanle.danci.feature.me

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.security.buildAiCredentialStore
import kotlinx.coroutines.launch

@Composable
fun AiSettingsRoute() {
    val context = LocalContext.current
    val viewModel = remember(context) {
        AiSettingsViewModel(
            settingsRepository = buildSettingsRepository(context),
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

    AiSettingsScreen(
        state = state,
        onEnabledChange = { state = state.copy(isEnabled = it) },
        onBaseUrlChange = { state = state.copy(baseUrl = it) },
        onModelChange = { state = state.copy(model = it) },
        onApiKeyChange = { state = state.copy(apiKeyInput = it) },
        onPlanAdjustmentsChange = { state = state.copy(enablePlanAdjustments = it) },
        onSessionCheckpointsChange = { state = state.copy(enableSessionCheckpoints = it) },
        onSaveClick = {
            scope.launch {
                state = state.copy(isSaving = true, statusMessage = null)
                state = viewModel.save(state).copy(isSaving = false)
            }
        },
    )
}
