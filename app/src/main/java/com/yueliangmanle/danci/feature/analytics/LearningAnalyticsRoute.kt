package com.yueliangmanle.danci.feature.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

const val LEARNING_ANALYTICS_ROUTE = "learning_analytics"

@Composable
fun LearningAnalyticsRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: LearningAnalyticsViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(LearningAnalyticsUiState(isLoading = true))
    }

    fun refresh() {
        scope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            state = runCatching {
                val resolvedViewModel = viewModel ?: loadLearningAnalyticsViewModel(context).also {
                    viewModel = it
                }
                resolvedViewModel.loadUiState()
            }.getOrElse { error ->
                LearningAnalyticsUiState(
                    title = "学习统计",
                    overviewCards = emptyList(),
                    chartHtml = null,
                    insightBullets = emptyList(),
                    errorMessage = error.message ?: "统计页暂时加载失败，请稍后重试。",
                )
            }
        }
    }

    LaunchedEffect(context) {
        refresh()
    }

    LearningAnalyticsScreen(
        state = state,
        onRefreshClick = ::refresh,
    )
}
