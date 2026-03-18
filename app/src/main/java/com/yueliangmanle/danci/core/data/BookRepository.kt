package com.yueliangmanle.danci.core.data

import com.yueliangmanle.danci.core.database.dao.BookDao
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.importer.ImportedBook
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.Word
import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

interface BookRepository {
    fun observeBooks(): Flow<List<Book>>
    fun observeWords(bookId: String): Flow<List<Word>>
    suspend fun upsertBook(book: Book)
    suspend fun addWordToBook(
        bookId: String,
        wordId: Long,
        chapter: String? = null,
        sortOrder: Int = 0,
        tags: List<String> = emptyList(),
        note: String? = null,
    )
    fun loadBuiltInCatalog(inputStream: InputStream): List<BuiltInBookCatalogItem>
    suspend fun importBook(book: ImportedBook, wordIds: List<Long>): Book
}

data class BuiltInBookCatalogItem(
    val id: String,
    val title: String,
    val description: String,
    val wordCount: Int,
    val assetName: String,
)

class RoomBookRepository(
    private val bookDao: BookDao,
) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeBooks().map { books -> books.map(BookEntity::asExternalModel) }

    override fun observeWords(bookId: String): Flow<List<Word>> =
        bookDao.observeWordsForBook(bookId).map { words -> words.map { it.asExternalModel() } }

    override suspend fun upsertBook(book: Book) {
        bookDao.insertBook(book.asEntity())
    }

    override suspend fun addWordToBook(
        bookId: String,
        wordId: Long,
        chapter: String?,
        sortOrder: Int,
        tags: List<String>,
        note: String?,
    ) {
        bookDao.insertBookWordCrossRef(
            BookWordEntity(
                bookId = bookId,
                wordId = wordId,
                chapter = chapter,
                sortOrder = sortOrder,
                tags = tags,
                note = note,
            ),
        )
    }

    override fun loadBuiltInCatalog(inputStream: InputStream): List<BuiltInBookCatalogItem> {
        return parseBuiltInCatalog(inputStream)
    }

    override suspend fun importBook(book: ImportedBook, wordIds: List<Long>): Book {
        val importedBook = book.metadata.asBook(wordCount = wordIds.size)
        upsertBook(importedBook)
        wordIds.forEachIndexed { index, wordId ->
            addWordToBook(
                bookId = importedBook.id,
                wordId = wordId,
                sortOrder = index,
            )
        }
        return importedBook
    }
}

internal fun BookEntity.asExternalModel(): Book =
    Book(
        id = id,
        title = title,
        description = description,
        language = language,
        category = category,
        sourceType = sourceType,
        wordCount = wordCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Book.asEntity(): BookEntity =
    BookEntity(
        id = id,
        title = title,
        description = description,
        language = language,
        category = category,
        sourceType = sourceType,
        wordCount = wordCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun BuiltInBookCatalogItem.asBook(): Book =
    Book(
        id = id,
        title = title,
        description = description,
        wordCount = wordCount,
        sourceType = "builtin",
    )

internal fun com.yueliangmanle.danci.core.importer.ImportedBookMetadata.asBook(wordCount: Int): Book =
    Book(
        id = id,
        title = title,
        description = description,
        language = language,
        category = category,
        sourceType = sourceType,
        wordCount = wordCount,
    )

fun parseBuiltInCatalog(inputStream: InputStream): List<BuiltInBookCatalogItem> {
    val root = JSONObject(inputStream.bufferedReader().use { it.readText() })
    val books = root.getJSONArray("books")
    return List(books.length()) { index ->
        val item = books.getJSONObject(index)
        BuiltInBookCatalogItem(
            id = item.getString("id"),
            title = item.getString("title"),
            description = item.optString("description"),
            wordCount = item.optInt("wordCount", 0),
            assetName = item.optString("asset"),
        )
    }
}
