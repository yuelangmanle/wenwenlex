package com.yueliangmanle.danci.feature.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.ai.resolveRuntimeSettingsForCapability
import com.yueliangmanle.danci.core.ai.buildAiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.loadCurrentPlanSnapshot
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.model.AiCapability
import kotlinx.coroutines.launch

@Composable
fun StudyRoute(
    onOpenDetailClick: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember(context) {
        buildSettingsRepository(context)
    }
    var viewModel: StudyViewModel? by remember(context) { mutableStateOf(null) }
    var state by remember(viewModel) {
        mutableStateOf(StudyUiState())
    }

    androidx.compose.runtime.LaunchedEffect(context) {
        val loaded = loadStudyViewModel(context)
        viewModel = loaded
        state = loaded.buildUiState()
    }

    StudyScreen(
        state = state,
        onFeedbackClick = { feedback ->
            val currentViewModel = viewModel ?: return@StudyScreen
            state = currentViewModel.submitFeedback(feedback)
            scope.launch {
                val settings = settingsRepository.getSettings()
                val checkpoint = currentViewModel.consumeCheckpointRequest(
                    sessionCheckpointsEnabled = settings.aiSessionCheckpointEnabled,
                ) ?: return@launch
                val snapshot = loadCurrentPlanSnapshot(
                    context = context,
                    activeBookTitle = "当前学习会话",
                    headline = state.progressText,
                    mistakeCount = checkpoint.mistakeBurst,
                    anomalyNotes = listOf(checkpoint.reason),
                )
                val result = buildAiStrategyCoordinator(context).adjustPlan(snapshot)
                state = currentViewModel.applyCheckpointSuggestion(result)
            }
        },
        onOpenDetailClick = {
            viewModel?.openCurrentWordDetail()
            onOpenDetailClick(state.currentWordId)
        },
    )
}
