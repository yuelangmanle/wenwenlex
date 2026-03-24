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

const val AUDIO_TASK_CENTER_ROUTE = "audio_task_center"

@Composable
fun AudioTaskCenterRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: AudioTaskCenterViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(AudioTaskCenterUiState.loading())
    }

    LaunchedEffect(context) {
        state = runCatching {
            val loaded = loadAudioTaskCenterViewModel(context)
            viewModel = loaded
            loaded.loadUiState()
        }.getOrElse { error ->
            AudioTaskCenterUiState(
                errorMessage = error.message ?: "音频任务中心加载失败，请稍后重试。",
            )
        }
    }

    fun launchAction(action: suspend AudioTaskCenterViewModel.() -> AudioTaskCenterUiState) {
        val currentViewModel = viewModel ?: return
        scope.launch {
            state = state.copy(isLoading = true)
            state = runCatching {
                currentViewModel.action()
            }.getOrElse { error ->
                currentViewModel.loadUiState(
                    selectedScopeType = state.selectedScopeType,
                    errorMessage = error.message ?: "操作失败，请稍后重试。",
                )
            }
        }
    }

    AudioTaskCenterScreen(
        state = state,
        onSelectScope = { scopeType ->
            launchAction {
                loadUiState(selectedScopeType = scopeType)
            }
        },
        onCreateJob = { jobType ->
            launchAction {
                createJob(
                    jobType = jobType,
                    selectedScopeType = state.selectedScopeType,
                )
            }
        },
        onPause = { jobId ->
            launchAction { pauseJob(jobId, state.selectedScopeType) }
        },
        onResume = { jobId ->
            launchAction { resumeJob(jobId, state.selectedScopeType) }
        },
        onCancel = { jobId ->
            launchAction { cancelJob(jobId, state.selectedScopeType) }
        },
    )
}
