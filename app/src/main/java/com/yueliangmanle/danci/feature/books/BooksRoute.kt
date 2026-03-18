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
        val viewModel = loadBooksViewModel(context)
        state = viewModel.loadUiState()
    }

    BooksScreen(
        state = state,
        onImportClick = onImportClick,
        onBookClick = onBookClick,
    )
}
