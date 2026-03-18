package com.yueliangmanle.danci.feature.books

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun BooksRoute(
    onImportClick: () -> Unit = {},
    onBookClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val state = remember(context) {
        loadBooksViewModel(context).buildUiState()
    }

    BooksScreen(
        state = state,
        onImportClick = onImportClick,
        onBookClick = onBookClick,
    )
}
