package com.yueliangmanle.danci.feature.aiplan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

fun planExplanationRoute(planVersionId: Long): String = "plan_explanation/$planVersionId"

@Composable
fun PlanExplanationRoute(
    planVersionId: Long,
) {
    val context = LocalContext.current
    var state by remember(planVersionId) {
        mutableStateOf(PlanExplanationUiState())
    }

    LaunchedEffect(context, planVersionId) {
        val viewModel = loadPlanExplanationViewModel(context)
        state = viewModel.loadUiState(planVersionId)
    }

    PlanExplanationScreen(state = state)
}
