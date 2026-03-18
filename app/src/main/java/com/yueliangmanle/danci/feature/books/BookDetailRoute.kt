package com.yueliangmanle.danci.feature.books

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

fun bookDetailRoute(bookId: String): String = "book_detail/${Uri.encode(bookId)}"

@Composable
fun BookDetailRoute(
    bookId: String,
    onWordClick: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember(context) { buildBookDetailViewModel(context) }
    var state by remember(bookId) {
        mutableStateOf(BookDetailUiState(isLoading = true))
    }

    LaunchedEffect(viewModel, bookId) {
        state = viewModel.loadUiState(bookId)
    }

    BookDetailScreen(
        state = state,
        onSetActiveBookClick = {
            scope.launch {
                state = viewModel.setActiveBook()
            }
        },
        onFillMissingPhoneticsClick = {
            scope.launch {
                state = state.copy(isFilling = true, errorMessage = null, statusMessage = null)
                state = viewModel.fillBookPhonetics(overwrite = false)
            }
        },
        onOverwritePhoneticsClick = {
            scope.launch {
                state = state.copy(isFilling = true, errorMessage = null, statusMessage = null)
                state = viewModel.fillBookPhonetics(overwrite = true)
            }
        },
        onWordClick = onWordClick,
    )
}
