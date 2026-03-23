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

const val PRONUNCIATION_SOURCE_DETAIL_ROUTE = "pronunciation_source_detail/{sourceId}"

fun pronunciationSourceDetailRoute(sourceId: String): String =
    "pronunciation_source_detail/$sourceId"

@Composable
fun PronunciationSourceDetailRoute(
    sourceId: String,
    onOpenTaskCenterClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: PronunciationSourceDetailViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(PronunciationSourceDetailUiState(isLoading = true))
    }

    LaunchedEffect(context, sourceId) {
        state = state.copy(isLoading = true)
        state = runCatching {
            val loaded = loadPronunciationSourceDetailViewModel(context)
            viewModel = loaded
            loaded.loadUiState(sourceId)
        }.getOrElse { error ->
            PronunciationSourceDetailUiState(
                errorMessage = error.message ?: "发音源详情加载失败，请稍后重试。",
            )
        }
    }

    fun launchAction(action: suspend PronunciationSourceDetailViewModel.() -> PronunciationSourceDetailUiState) {
        val currentViewModel = viewModel ?: return
        scope.launch {
            state = state.copy(isLoading = true)
            state = runCatching {
                currentViewModel.action()
            }.getOrElse { error ->
                currentViewModel.loadUiState(
                    sourceId = sourceId,
                    errorMessage = error.message ?: "操作失败，请稍后重试。",
                )
            }
        }
    }

    PronunciationSourceDetailScreen(
        state = state,
        onBindProfileClick = { profileId ->
            launchAction { bindProfile(sourceId, profileId) }
        },
        onSelectPresetClick = { presetId ->
            launchAction { selectPreset(sourceId, presetId) }
        },
        onAdvancedStyleChange = { text ->
            state = viewModel?.updateAdvancedStyle(state, text) ?: state
        },
        onTestTextChange = { text ->
            state = viewModel?.updateTestText(state, text) ?: state
        },
        onGenerationWordTextChange = { text ->
            state = viewModel?.updateGenerationWordText(state, text) ?: state
        },
        onGenerationBatchSizeTextChange = { text ->
            state = viewModel?.updateGenerationBatchSizeText(state, text) ?: state
        },
        onCheckApiClick = {
            launchAction { checkApi(sourceId, state) }
        },
        onGenerateSingleWordClick = {
            launchAction { enqueueSingleWordGeneration(sourceId, state) }
        },
        onGenerateBookClick = { bookId ->
            launchAction { enqueueBookGeneration(sourceId, bookId, state) }
        },
        onGenerateBatchClick = { bookId ->
            launchAction { enqueueBookBatchGeneration(sourceId, bookId, state) }
        },
        onOpenTaskCenterClick = onOpenTaskCenterClick,
    )
}
