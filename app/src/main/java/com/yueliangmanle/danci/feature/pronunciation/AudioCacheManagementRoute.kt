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

const val AUDIO_CACHE_MANAGEMENT_ROUTE = "audio_cache_management"

@Composable
fun AudioCacheManagementRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: AudioCacheManagementViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(AudioCacheManagementUiState(isLoading = true))
    }

    LaunchedEffect(context) {
        state = runCatching {
            val loaded = loadAudioCacheManagementViewModel(context)
            viewModel = loaded
            loaded.loadUiState()
        }.getOrElse { error ->
            AudioCacheManagementUiState(
                errorMessage = error.message ?: "缓存管理加载失败，请稍后重试。",
            )
        }
    }

    AudioCacheManagementScreen(
        state = state,
        onClearCacheClick = {
            val currentViewModel = viewModel
            if (currentViewModel != null) {
                scope.launch {
                    state = state.copy(isLoading = true)
                    state = runCatching {
                        currentViewModel.clearDictionaryCache()
                    }.getOrElse { error ->
                        currentViewModel.loadUiState(errorMessage = error.message ?: "清理缓存失败，请稍后重试。")
                    }
                }
            }
        },
        onClearBucketClick = { sourceType ->
            val currentViewModel = viewModel
            if (currentViewModel != null) {
                scope.launch {
                    state = state.copy(isLoading = true)
                    state = runCatching {
                        currentViewModel.clearCacheBucket(sourceType)
                    }.getOrElse { error ->
                        currentViewModel.loadUiState(errorMessage = error.message ?: "清理这类缓存失败，请稍后重试。")
                    }
                }
            }
        },
        onClearAllCachesClick = {
            val currentViewModel = viewModel
            if (currentViewModel != null) {
                scope.launch {
                    state = state.copy(isLoading = true)
                    state = runCatching {
                        currentViewModel.clearAllCaches()
                    }.getOrElse { error ->
                        currentViewModel.loadUiState(errorMessage = error.message ?: "清理全部缓存失败，请稍后重试。")
                    }
                }
            }
        },
    )
}
