package com.yueliangmanle.danci.feature.pronunciation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

const val CLOUD_TTS_SETTINGS_ROUTE = "cloud_tts_settings"

@Composable
fun CloudTtsSettingsRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: CloudTtsSettingsViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(CloudTtsSettingsUiState(isLoading = true))
    }

    LaunchedEffect(context) {
        state = runCatching {
            val loaded = loadCloudTtsSettingsViewModel(context)
            viewModel = loaded
            loaded.loadUiState()
        }.getOrElse { error ->
            CloudTtsSettingsUiState(
                errorMessage = error.message ?: "云端 TTS 设置加载失败，请稍后重试。",
            )
        }
    }

    fun launchAction(action: suspend CloudTtsSettingsViewModel.() -> CloudTtsSettingsUiState) {
        val currentViewModel = viewModel ?: return
        scope.launch {
            state = state.copy(isLoading = true, statusMessage = null, errorMessage = null)
            state = runCatching {
                currentViewModel.action()
            }.getOrElse { error ->
                currentViewModel.loadUiState(errorMessage = error.message ?: "操作失败，请稍后重试。")
            }.copy(
                isLoading = false,
                apiKeyInput = state.apiKeyInput,
            )
        }
    }

    CloudTtsSettingsScreen(
        state = state,
        onSelectProvider = { providerId ->
            launchAction { selectProvider(providerId) }
        },
        onSelectPreset = { presetId ->
            launchAction { selectPreset(presetId) }
        },
        onBaseUrlChange = { baseUrl ->
            state = state.copy(baseUrl = baseUrl)
        },
        onModelChange = { model ->
            state = state.copy(model = model)
        },
        onSaveEndpointClick = {
            val baseUrl = state.baseUrl
            val model = state.model
            launchAction {
                updateBaseUrl(baseUrl)
                updateModel(model)
            }
        },
        onApiKeyChange = { apiKey ->
            state = state.copy(apiKeyInput = apiKey)
        },
        onSaveApiKeyClick = {
            state.defaultProviderId?.let { providerId ->
                val apiKey = state.apiKeyInput
                launchAction { saveApiKey(providerId, apiKey) }
            }
        },
        onClearApiKeyClick = {
            state.defaultProviderId?.let { providerId ->
                launchAction { clearApiKey(providerId) }
            }
        },
        onCheckApiClick = {
            launchAction { checkHealth(state) }
        },
    )
}
