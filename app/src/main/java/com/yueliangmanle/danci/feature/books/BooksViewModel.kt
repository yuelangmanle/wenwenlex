package com.yueliangmanle.danci.feature.books

import android.content.Context
import com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem
import com.yueliangmanle.danci.core.data.parseBuiltInCatalog
import com.yueliangmanle.danci.core.model.Book

data class BooksUiState(
    val builtInBooks: List<BookListItem> = emptyList(),
    val importedBooks: List<BookListItem> = emptyList(),
)

data class BookListItem(
    val id: String,
    val title: String,
    val description: String,
    val wordCount: Int,
    val sourceType: String,
)

class BooksViewModel(
    private val builtInBooks: List<BuiltInBookCatalogItem>,
    private val importedBooks: List<Book> = emptyList(),
) {
    fun buildUiState(): BooksUiState =
        BooksUiState(
            builtInBooks = builtInBooks.map(BuiltInBookCatalogItem::asUiModel),
            importedBooks = importedBooks.map(Book::asUiModel),
        )
}

fun loadBooksViewModel(context: Context): BooksViewModel {
    val builtInBooks = context.assets.open("books/manifest.json").use(::parseBuiltInCatalog)
    return BooksViewModel(builtInBooks = builtInBooks)
}

private fun BuiltInBookCatalogItem.asUiModel(): BookListItem =
    BookListItem(
        id = id,
        title = title,
        description = description,
        wordCount = wordCount,
        sourceType = "内置",
    )

private fun Book.asUiModel(): BookListItem =
    BookListItem(
        id = id,
        title = title,
        description = description.orEmpty(),
        wordCount = wordCount,
        sourceType = "导入",
    )
