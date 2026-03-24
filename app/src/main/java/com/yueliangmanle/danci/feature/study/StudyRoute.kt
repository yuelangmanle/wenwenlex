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
import com.yueliangmanle.danci.core.pronunciation.buildPronunciationOrchestrator
import com.yueliangmanle.danci.core.study.StudyLaunchMode
import kotlinx.coroutines.launch

@Composable
fun StudyRoute(
    launchMode: StudyLaunchMode? = null,
    onOpenDetailClick: (Long) -> Unit = {},
    onBackHomeClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember(context) {
        buildSettingsRepository(context)
    }
    val pronunciationOrchestrator = remember(context) { buildPronunciationOrchestrator(context) }
    var viewModel: StudyViewModel? by remember(context) { mutableStateOf(null) }
    var resolvedMode by remember(launchMode) { mutableStateOf(launchMode) }
    var state by remember(launchMode) {
        mutableStateOf(StudyUiState())
    }
    val sessionLabel = remember(resolvedMode) {
        when (resolvedMode) {
            StudyLaunchMode.NEW_WORDS -> "新词学习会话"
            StudyLaunchMode.REVIEW -> "复习学习会话"
            StudyLaunchMode.RECENT_MISTAKES -> "错题学习会话"
            null -> "当前学习会话"
        }
    }

    androidx.compose.runtime.LaunchedEffect(context, launchMode) {
        resolvedMode = launchMode
        state = StudyUiState(sessionTitle = sessionLabel)
        val loaded = loadStudyViewModel(context, launchMode)
        viewModel = loaded.viewModel
        resolvedMode = loaded.resolvedMode
        state = loaded.viewModel.buildUiState().copy(sessionTitle = sessionLabelFor(loaded.resolvedMode))
    }

    StudyScreen(
        state = state,
        onFeedbackClick = { feedback ->
            val currentViewModel = viewModel ?: return@StudyScreen
            state = currentViewModel.submitFeedback(feedback).copy(sessionTitle = sessionLabel)
            scope.launch {
                val settings = settingsRepository.getSettings()
                val checkpoint = currentViewModel.consumeCheckpointRequest(
                    sessionCheckpointsEnabled = settings.aiSessionCheckpointEnabled,
                ) ?: return@launch
                val snapshot = loadCurrentPlanSnapshot(
                    context = context,
                    activeBookTitle = sessionLabel,
                    headline = state.progressText,
                    mistakeCount = checkpoint.mistakeBurst,
                    anomalyNotes = listOf(checkpoint.reason),
                )
                val result = buildAiStrategyCoordinator(context).adjustPlan(snapshot)
                state = currentViewModel.applyCheckpointSuggestion(result).copy(sessionTitle = sessionLabel)
            }
        },
        onOpenDetailClick = {
            viewModel?.openCurrentWordDetail()
            onOpenDetailClick(state.currentWordId)
        },
        onBackHomeClick = onBackHomeClick,
        onPlayPronunciationClick = {
            scope.launch {
                val result = pronunciationOrchestrator.playWordById(
                    wordId = state.currentWordId,
                    contextLabel = sessionLabel,
                )
                state = state.copy(
                    statusMessage = result.statusMessage,
                    errorMessage = result.errorMessage,
                )
            }
        },
        onContinueNextGroupClick = {
            val currentViewModel = viewModel ?: return@StudyScreen
            state = currentViewModel.continueNextGroup().copy(sessionTitle = sessionLabel)
        },
    )
}

private fun sessionLabelFor(mode: StudyLaunchMode?): String =
    when (mode) {
        StudyLaunchMode.NEW_WORDS -> "新词学习会话"
        StudyLaunchMode.REVIEW -> "复习学习会话"
        StudyLaunchMode.RECENT_MISTAKES -> "错题学习会话"
        null -> "当前学习会话"
    }
