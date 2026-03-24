package com.yueliangmanle.danci.feature.pronunciation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.data.AudioCacheFilter
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
        mutableStateOf(AudioCacheManagementUiState.loading())
    }

    LaunchedEffect(context) {
        state = runCatching {
            val loaded = loadAudioCacheManagementViewModel(context)
            viewModel = loaded
            loaded.loadUiState()
        }.getOrElse { error ->
            AudioCacheManagementUiState(
                errorMessage = error.message ?: "音频缓存管理加载失败，请稍后重试。",
            )
        }
    }

    fun launchLoad(
        filter: AudioCacheFilter = state.filter,
        action: suspend AudioCacheManagementViewModel.(AudioCacheFilter) -> AudioCacheManagementUiState,
    ) {
        val currentViewModel = viewModel ?: return
        scope.launch {
            state = state.copy(isLoading = true)
            state = runCatching {
                currentViewModel.action(filter)
            }.getOrElse { error ->
                currentViewModel.loadUiState(
                    filter = filter,
                    errorMessage = error.message ?: "操作失败，请稍后重试。",
                )
            }
        }
    }

    AudioCacheManagementScreen(
        state = state,
        onSelectSource = { sourceType ->
            val nextSource = if (state.filter.sourceType == sourceType) null else sourceType
            launchLoad(state.filter.copy(sourceType = nextSource)) { filter ->
                loadUiState(filter)
            }
        },
        onSelectBook = { bookId ->
            val nextBookId = if (state.filter.bookId == bookId) null else bookId
            launchLoad(state.filter.copy(bookId = nextBookId)) { filter ->
                loadUiState(filter)
            }
        },
        onQueryChange = { query ->
            launchLoad(state.filter.copy(query = query)) { filter ->
                loadUiState(filter)
            }
        },
        onDecreaseLimit = {
            launchLoad(state.filter) { filter ->
                updateCacheLimit((state.cacheLimitMb - 50).coerceAtLeast(50), filter)
            }
        },
        onIncreaseLimit = {
            launchLoad(state.filter) { filter ->
                updateCacheLimit((state.cacheLimitMb + 50).coerceAtMost(2048), filter)
            }
        },
        onTrimToLimit = {
            launchLoad(state.filter) { filter ->
                trimToConfiguredLimit(filter)
            }
        },
        onClearBucket = { sourceType ->
            launchLoad(state.filter) { filter ->
                clearBucket(sourceType, filter)
            }
        },
    )
}
