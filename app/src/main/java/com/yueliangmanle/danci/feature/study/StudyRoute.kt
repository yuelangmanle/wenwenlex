package com.yueliangmanle.danci.feature.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun StudyRoute(
    onOpenDetailClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = remember(context) {
        loadStudyViewModel(context)
    }
    var state by remember(viewModel) {
        mutableStateOf(viewModel.buildUiState())
    }

    StudyScreen(
        state = state,
        onFeedbackClick = { feedback ->
            state = viewModel.submitFeedback(feedback)
        },
        onOpenDetailClick = onOpenDetailClick,
    )
}
