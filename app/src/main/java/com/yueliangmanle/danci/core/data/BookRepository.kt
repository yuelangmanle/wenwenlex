package com.yueliangmanle.danci.core.data

import com.yueliangmanle.danci.core.database.dao.BookDao
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}

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
