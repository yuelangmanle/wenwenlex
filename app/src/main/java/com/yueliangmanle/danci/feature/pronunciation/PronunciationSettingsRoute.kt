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

const val PRONUNCIATION_SETTINGS_ROUTE = "pronunciation_settings"

@Composable
fun PronunciationSettingsRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: PronunciationSettingsViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(PronunciationSettingsUiState(isLoading = true))
    }

    LaunchedEffect(context) {
        state = runCatching {
            val loaded = loadPronunciationSettingsViewModel(context)
            viewModel = loaded
            loaded.loadUiState()
        }.getOrElse { error ->
            PronunciationSettingsUiState(
                errorMessage = error.message ?: "发音设置加载失败，请稍后重试。",
            )
        }
    }

    fun launchAction(action: suspend PronunciationSettingsViewModel.() -> PronunciationSettingsUiState) {
        val currentViewModel = viewModel ?: return
        scope.launch {
            state = runCatching {
                currentViewModel.action()
            }.getOrElse { error ->
                currentViewModel.loadUiState(errorMessage = error.message ?: "操作失败，请稍后重试。")
            }
        }
    }

    PronunciationSettingsScreen(
        state = state,
        onSelectAccent = { accent ->
            launchAction { updatePreferredAccent(accent) }
        },
        onSelectMode = { mode ->
            launchAction { updatePronunciationMode(mode) }
        },
        onAutoCacheChanged = { enabled ->
            launchAction { updateAutoCache(enabled) }
        },
        onAllowCellularChanged = { enabled ->
            launchAction { updateAllowCellular(enabled) }
        },
        onFallbackToSystemTtsChanged = { enabled ->
            launchAction { updateFallbackToSystemTts(enabled) }
        },
        onPreferOfflineLongTextChanged = { enabled ->
            launchAction { updatePreferOfflineForLongText(enabled) }
        },
        onClearCacheClick = {
            launchAction { clearDictionaryCache() }
        },
        onActivateVoicePack = { packId ->
            launchAction { activateVoicePack(packId) }
        },
    )
}

