package com.yueliangmanle.danci.feature.releasenotes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

const val RELEASE_NOTES_ROUTE = "release_notes"

@Composable
fun ReleaseNotesRoute() {
    val context = LocalContext.current
    var state by remember {
        mutableStateOf(ReleaseNotesUiState(isLoading = true))
    }

    LaunchedEffect(context) {
        state = runCatching {
            loadReleaseNotesViewModel(context).loadUiState()
        }.getOrElse { error ->
            ReleaseNotesUiState(
                errorMessage = error.message ?: "更新日志加载失败，请稍后重试。",
            )
        }
    }

    ReleaseNotesScreen(state = state)
}
