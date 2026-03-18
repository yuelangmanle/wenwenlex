package com.yueliangmanle.danci.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeRoute(
    onStartNewWordsClick: () -> Unit = {},
    onStartReviewClick: () -> Unit = {},
    onOpenMistakesClick: () -> Unit = {},
    onAnalyzePlanClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val state by produceState(
        initialValue = HomeUiState.loading(),
        key1 = context,
    ) {
        value = loadHomeViewModel(context).buildUiState()
    }

    HomeScreen(
        state = state,
        onStartNewWordsClick = onStartNewWordsClick,
        onStartReviewClick = onStartReviewClick,
        onOpenMistakesClick = onOpenMistakesClick,
        onAnalyzePlanClick = onAnalyzePlanClick,
    )
}
