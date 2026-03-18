package com.yueliangmanle.danci.feature.worddetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

fun wordDetailRoute(wordId: Long): String = "word_detail/$wordId"

@Composable
fun WordDetailRoute(
    wordId: Long,
    onAiMemoryClick: () -> Unit = {},
    onAiContrastClick: () -> Unit = {},
    onStartQuizClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = remember(context, wordId) {
        loadWordDetailViewModel(context, wordId)
    }
    var state by remember(viewModel) {
        mutableStateOf(viewModel.buildUiState())
    }

    WordDetailScreen(
        state = state,
        onAiMemoryClick = {
            viewModel.onAiMemoryClick()
            onAiMemoryClick()
        },
        onAiContrastClick = {
            viewModel.onAiContrastClick()
            onAiContrastClick()
        },
        onStartQuizClick = {
            viewModel.onStartQuizClick()
            onStartQuizClick()
        },
    )
}
