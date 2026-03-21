package com.yueliangmanle.danci.feature.aiplan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

fun planComparisonRoute(planVersionId: Long): String = "plan_comparison/$planVersionId"

@Composable
fun PlanComparisonRoute(
    planVersionId: Long,
) {
    val context = LocalContext.current
    var state by remember(planVersionId) {
        mutableStateOf(PlanComparisonUiState())
    }

    LaunchedEffect(context, planVersionId) {
        val viewModel = loadPlanComparisonViewModel(context)
        state = viewModel.loadUiState(planVersionId)
    }

    PlanComparisonScreen(state = state)
}
