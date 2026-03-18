package com.yueliangmanle.danci.feature.quiz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.ai.buildAiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.buildRuntimeSettings
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.security.buildAiCredentialStore
import kotlinx.coroutines.launch

fun quizRoute(wordId: Long): String = "quiz/$wordId"

@Composable
fun QuizRoute(
    wordId: Long,
    onOpenDetailClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember(context, wordId) {
        loadQuizViewModel(context, wordId)
    }
    var state by remember(viewModel) {
        mutableStateOf(viewModel.buildUiState())
    }

    QuizScreen(
        state = state,
        onOptionClick = { option ->
            state = viewModel.selectOption(option)
            if (viewModel.needsMistakeInsight()) {
                scope.launch {
                    val settings = buildSettingsRepository(context).getSettings()
                    state = viewModel.resolveMistakeInsight(
                        settings = settings,
                        runtimeSettings = buildRuntimeSettings(settings, buildAiCredentialStore(context)),
                        coordinator = buildAiStrategyCoordinator(context),
                    )
                }
            }
        },
        onOpenDetailClick = {
            viewModel.openWordDetail()
            onOpenDetailClick()
        },
    )
}
