package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookWordCrossRef(crossRef: BookWordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookWordCrossRefs(crossRefs: List<BookWordEntity>)

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT words.* FROM words INNER JOIN book_words ON words.id = book_words.wordId WHERE book_words.bookId = :bookId ORDER BY book_words.sortOrder ASC, words.lemma ASC")
    suspend fun getWordsForBook(bookId: String): List<WordEntity>

    @Query("SELECT COUNT(*) FROM book_words WHERE bookId = :bookId")
    suspend fun countWordsForBook(bookId: String): Int

    @Query("SELECT * FROM book_words ORDER BY bookId ASC, sortOrder ASC, wordId ASC")
    suspend fun getAllBookWordCrossRefs(): List<BookWordEntity>

    @Query(
        """
        SELECT words.* FROM words
        INNER JOIN book_words ON words.id = book_words.wordId
        WHERE book_words.bookId = :bookId
        ORDER BY book_words.sortOrder ASC, words.lemma ASC
        """,
    )
    fun observeWordsForBook(bookId: String): Flow<List<WordEntity>>

    @Query("DELETE FROM book_words WHERE bookId = :bookId")
    suspend fun clearBookWordCrossRefsForBook(bookId: String)

    @Query("DELETE FROM book_words")
    suspend fun clearBookWordCrossRefs()

    @Query("DELETE FROM books")
    suspend fun clearBooks()
}
