package com.yueliangmanle.danci.feature.books

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

const val BOOK_IMPORT_ROUTE = "book_import"

@Composable
fun BookImportRoute(
    onImportedBook: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember(context) { buildBookImportViewModel(context) }
    var state by remember {
        mutableStateOf(BookImportUiState())
    }

    val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            state = state.copy(isLoading = true, errorMessage = null, statusMessage = null)
            val fileName = queryDisplayName(context, uri) ?: "import.xlsx"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            state = if (bytes == null) {
                state.copy(
                    isLoading = false,
                    errorMessage = "读取文件失败，请重新选择。",
                )
            } else {
                viewModel.loadFromBytes(fileName, bytes)
            }
        }
    }

    BookImportScreen(
        state = state,
        onBookTitleChange = { title ->
            state = viewModel.updateBookTitle(state, title)
        },
        onPickFileClick = {
            launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        },
        onAiNormalizeClick = {
            scope.launch {
                state = state.copy(isAiNormalizing = true, errorMessage = null, statusMessage = null)
                state = viewModel.normalizeWithAi(state)
            }
        },
        onImportClick = {
            scope.launch {
                state = state.copy(isImporting = true, errorMessage = null, statusMessage = null)
                state = viewModel.importCurrent(state)
                state.importedBookId?.let(onImportedBook)
            }
        },
    )
}

private fun queryDisplayName(
    context: Context,
    uri: Uri,
): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
