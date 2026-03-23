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

const val AUDIO_GENERATION_TASKS_ROUTE = "audio_generation_tasks"

@Composable
fun AudioGenerationTasksRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: AudioGenerationTasksViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(AudioGenerationTasksUiState(isLoading = true))
    }

    LaunchedEffect(context) {
        state = runCatching {
            val loaded = loadAudioGenerationTasksViewModel(context)
            viewModel = loaded
            loaded.loadUiState()
        }.getOrElse { error ->
            AudioGenerationTasksUiState(
                errorMessage = error.message ?: "任务中心加载失败，请稍后重试。",
            )
        }
    }

    AudioGenerationTasksScreen(
        state = state,
        onRetryFailedItemsClick = { taskId ->
            val currentViewModel = viewModel ?: return@AudioGenerationTasksScreen
            scope.launch {
                state = state.copy(isLoading = true)
                state = runCatching {
                    currentViewModel.retryFailedItems(taskId)
                }.getOrElse { error ->
                    currentViewModel.loadUiState(
                        errorMessage = error.message ?: "重试失败任务时出错，请稍后再试。",
                    )
                }
            }
        },
    )
}
