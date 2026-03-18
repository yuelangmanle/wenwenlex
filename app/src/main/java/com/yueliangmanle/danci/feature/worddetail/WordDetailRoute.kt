package com.yueliangmanle.danci.feature.worddetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val state = remember(context, wordId) {
        loadWordDetailViewModel(context, wordId).buildUiState()
    }

    WordDetailScreen(
        state = state,
        onAiMemoryClick = onAiMemoryClick,
        onAiContrastClick = onAiContrastClick,
        onStartQuizClick = onStartQuizClick,
    )
}
