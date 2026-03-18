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
        val loadedViewModel = loadHomeViewModel(context)
        viewModel = loadedViewModel
        state = loadedViewModel.buildUiState()
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
                val snapshot = loadCurrentPlanSnapshot(
                    context = context,
                    activeBookTitle = state.activeBookTitle,
                    headline = state.headline,
                    mistakeCount = state.mistakeCount,
                    anomalyNotes = listOf("首页手动触发分析"),
                )
                val result = buildAiStrategyCoordinator(context).adjustPlan(snapshot)
                state = currentViewModel.applyPlanAdjustment(state, result)
            }
        },
    )
}
