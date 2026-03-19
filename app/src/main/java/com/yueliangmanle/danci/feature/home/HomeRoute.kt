package com.yueliangmanle.danci.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.ai.buildAiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.loadCurrentPlanSnapshot
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onStartNewWordsClick: () -> Unit = {},
    onStartReviewClick: () -> Unit = {},
    onOpenMistakesClick: () -> Unit = {},
    onAnalyzePlanClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: HomeViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(HomeUiState.loading())
    }

    LaunchedEffect(context) {
        state = runCatching {
            val loadedViewModel = loadHomeViewModel(context)
            viewModel = loadedViewModel
            loadedViewModel.buildUiState()
        }.getOrElse { error ->
            HomeUiState(
                activeBookTitle = "文文Lex",
                headline = "首页暂时没有加载出来",
                aiSuggestionTitle = "加载失败",
                aiSuggestion = "可以稍后重试，或先切到其他页面继续操作。",
                errorMessage = error.message ?: "首页加载失败，请稍后重试。",
            )
        }
    }

    HomeScreen(
        state = state,
        onStartNewWordsClick = onStartNewWordsClick,
        onStartReviewClick = onStartReviewClick,
        onOpenMistakesClick = onOpenMistakesClick,
        onAnalyzePlanClick = {
            onAnalyzePlanClick()
            scope.launch {
                val currentViewModel = viewModel ?: return@launch
                state = currentViewModel.markAnalyzing(state)
                state = runCatching {
                    val snapshot = loadCurrentPlanSnapshot(
                        context = context,
                        activeBookTitle = state.activeBookTitle,
                        headline = state.headline,
                        mistakeCount = state.mistakeCount,
                        anomalyNotes = listOf("首页手动触发分析"),
                    )
                    val result = buildAiStrategyCoordinator(context).adjustPlan(snapshot)
                    currentViewModel.applyPlanAdjustment(state, result)
                }.getOrElse { error ->
                    state.copy(
                        isAnalyzingPlan = false,
                        errorMessage = error.message ?: "AI 分析失败，请稍后重试。",
                        aiSuggestionTitle = "AI 分析失败",
                        aiSuggestion = "这次没有拿到可用结果，先按当前节奏学习即可。",
                        aiSuggestionMeta = null,
                    )
                }
            }
        },
    )
}
