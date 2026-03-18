package com.yueliangmanle.danci.feature.quiz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

fun quizRoute(wordId: Long): String = "quiz/$wordId"

@Composable
fun QuizRoute(
    wordId: Long,
    onOpenDetailClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = remember(context, wordId) {
        loadQuizViewModel(context, wordId)
    }
    var state by remember(viewModel) {
        mutableStateOf(viewModel.buildUiState())
    }

    QuizScreen(
        state = state,
        onOptionClick = { option ->
            state = viewModel.buildUiState(selectedOption = option)
        },
        onOpenDetailClick = onOpenDetailClick,
    )
}
