package com.yueliangmanle.danci.feature.aiplan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

const val AI_PLAN_CENTER_ROUTE = "ai_plan_center"

@Composable
fun AiPlanCenterRoute(
    onOpenComparisonClick: (Long) -> Unit = {},
    onOpenExplanationClick: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: AiPlanCenterViewModel? by remember(context) {
        mutableStateOf(null)
    }
    var state by remember {
        mutableStateOf(AiPlanCenterUiState(isLoading = true, emptyMessage = null))
    }

    LaunchedEffect(context) {
        state = runCatching {
            val loaded = loadAiPlanCenterViewModel(context)
            viewModel = loaded
            loaded.loadUiState()
        }.getOrElse { error ->
            AiPlanCenterUiState(
                errorMessage = error.message ?: "AI 计划中心加载失败，请稍后重试。",
            )
        }
    }

    fun launchAction(action: suspend AiPlanCenterViewModel.() -> AiPlanCenterUiState) {
        val currentViewModel = viewModel ?: return
        scope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            state = runCatching {
                currentViewModel.action()
            }.getOrElse { error ->
                currentViewModel.loadUiState(errorMessage = error.message ?: "操作失败，请稍后重试。")
            }
        }
    }

    AiPlanCenterScreen(
        state = state,
        onConfirmPendingPlanClick = { planId ->
            launchAction { confirmPendingPlan(planId) }
        },
        onRejectPendingPlanClick = { planId ->
            launchAction { rejectPendingPlan(planId) }
        },
        onOpenComparisonClick = onOpenComparisonClick,
        onOpenExplanationClick = onOpenExplanationClick,
    )
}
