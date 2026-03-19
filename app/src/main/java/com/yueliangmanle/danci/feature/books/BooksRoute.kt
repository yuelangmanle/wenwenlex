package com.yueliangmanle.danci.feature.books

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun BooksRoute(
    onImportClick: () -> Unit = {},
    onBookClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var state by remember {
        mutableStateOf(BooksUiState(isLoading = true))
    }

    LaunchedEffect(context) {
        state = runCatching {
            val viewModel = loadBooksViewModel(context)
            viewModel.loadUiState()
        }.getOrElse { error ->
            BooksUiState(
                statusMessage = "词书页加载失败，可以稍后重试。",
                errorMessage = error.message ?: "词书页加载失败，请稍后重试。",
            )
        }
    }

    BooksScreen(
        state = state,
        onImportClick = onImportClick,
        onBookClick = onBookClick,
    )
}
