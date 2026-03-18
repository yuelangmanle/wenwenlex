package com.yueliangmanle.danci.feature.quiz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.ai.buildAiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.resolveRuntimeSettingsForCapability
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.model.AiCapability
import kotlinx.coroutines.launch

fun quizRoute(wordId: Long): String = "quiz/$wordId"

@Composable
fun QuizRoute(
    wordId: Long,
    onOpenDetailClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewModel: QuizViewModel? by remember(context, wordId) {
        mutableStateOf(null)
    }
    var state by remember(wordId) {
        mutableStateOf(QuizUiState())
    }

    androidx.compose.runtime.LaunchedEffect(context, wordId) {
        val loaded = loadQuizViewModel(context, wordId)
        viewModel = loaded
        state = loaded.buildUiState()
    }

    QuizScreen(
        state = state,
        onOptionClick = { option ->
            val currentViewModel = viewModel ?: return@QuizScreen
            state = currentViewModel.selectOption(option)
            if (currentViewModel.needsMistakeInsight()) {
                scope.launch {
                    val settings = buildSettingsRepository(context).getSettings()
                    state = currentViewModel.resolveMistakeInsight(
                        settings = settings,
                        runtimeSettings = resolveRuntimeSettingsForCapability(context, AiCapability.WORD_HELP),
                        coordinator = buildAiStrategyCoordinator(context),
                    )
                }
            }
        },
        onOpenDetailClick = {
            viewModel?.openWordDetail()
            onOpenDetailClick()
        },
    )
}
