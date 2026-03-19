package com.yueliangmanle.danci.feature.books

import android.content.Context
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.parseBuiltInCatalog
import com.yueliangmanle.danci.core.data.syncBuiltInCatalogToDatabase
import com.yueliangmanle.danci.core.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BooksUiState(
    val isLoading: Boolean = false,
    val builtInBooks: List<BookListItem> = emptyList(),
    val importedBooks: List<BookListItem> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class BookListItem(
    val id: String,
    val title: String,
    val description: String,
    val wordCount: Int,
    val sourceType: String,
    val sourceMeta: String? = null,
    val isActive: Boolean = false,
)

class BooksViewModel(
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository,
    private val builtInCatalog: List<BuiltInBookCatalogItem>,
) {
    suspend fun loadUiState(
        statusMessage: String? = null,
    ): BooksUiState {
        val settings = settingsRepository.getSettings()
        val allBooks = bookRepository.getAllBooks()
        val booksById = allBooks.associateBy(Book::id)
        val builtInBooks = builtInCatalog.mapNotNull { item ->
            booksById[item.id]?.asUiModel(
                sourceTypeLabel = "内置词书",
                sourceMeta = listOfNotNull(
                    item.sourceName.takeIf(String::isNotBlank),
                    item.coverage,
                    item.sourceLicense.takeIf(String::isNotBlank),
                ).joinToString(" · ").ifBlank { null },
                isActive = settings.activeBookId == item.id,
            )
        }
        val importedBooks = allBooks
            .filter { it.sourceType != "builtin" }
            .map { book ->
                book.asUiModel(
                    sourceTypeLabel = "导入词书",
                    sourceMeta = "支持 Excel 导入与 AI 适配",
                    isActive = settings.activeBookId == book.id,
                )
            }

        return BooksUiState(
            builtInBooks = builtInBooks,
            importedBooks = importedBooks,
            statusMessage = statusMessage,
        )
    }
}

suspend fun loadBooksViewModel(context: Context): BooksViewModel {
    return withContext(Dispatchers.IO) {
        syncBuiltInCatalogToDatabase(context)
        val builtInCatalog = context.assets.open("books/manifest.json").use(::parseBuiltInCatalog)
        BooksViewModel(
            bookRepository = buildBookRepository(context),
            settingsRepository = buildSettingsRepository(context),
            builtInCatalog = builtInCatalog,
        )
    }
}

private fun Book.asUiModel(
    sourceTypeLabel: String,
    sourceMeta: String?,
    isActive: Boolean,
): BookListItem =
    BookListItem(
        id = id,
        title = title,
        description = description.orEmpty(),
        wordCount = wordCount,
        sourceType = sourceTypeLabel,
        sourceMeta = sourceMeta,
        isActive = isActive,
    )
