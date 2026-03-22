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
import com.yueliangmanle.danci.core.ai.PlanHistoryRepository
import com.yueliangmanle.danci.core.ai.loadCurrentPlanSnapshot
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.AiCapability
import com.yueliangmanle.danci.core.pronunciation.buildPronunciationOrchestrator
import kotlinx.coroutines.launch

@Composable
fun StudyRoute(
    onOpenDetailClick: (Long) -> Unit = {},
    onOpenPlanCenterClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember(context) {
        buildSettingsRepository(context)
    }
    val studyRepository = remember(context) {
        RoomStudyRepository(buildDanciDatabase(context).studyDao())
    }
    val planHistoryRepository = remember(context) {
        PlanHistoryRepository(studyRepository = studyRepository)
    }
    val pronunciationOrchestrator = remember(context) { buildPronunciationOrchestrator(context) }
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
                val version = planHistoryRepository.createCandidateVersion(snapshot, result)
                state = currentViewModel.applyCheckpointSuggestion(
                    adjustment = result,
                    version = version,
                )
            }
        },
        onSkipClick = {
            val currentViewModel = viewModel ?: return@StudyScreen
            state = currentViewModel.skipCurrentCard()
        },
        onOpenDetailClick = {
            viewModel?.openCurrentWordDetail()
            onOpenDetailClick(state.currentWordId)
        },
        onPlayPronunciationClick = {
            scope.launch {
                val result = pronunciationOrchestrator.playWordById(
                    wordId = state.currentWordId,
                    contextLabel = "study",
                )
                state = state.copy(
                    statusMessage = result.statusMessage,
                    errorMessage = result.errorMessage,
                )
            }
        },
        onOpenPlanCenterClick = onOpenPlanCenterClick,
    )
}
