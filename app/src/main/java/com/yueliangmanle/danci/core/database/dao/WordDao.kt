package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yueliangmanle.danci.core.database.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWord(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<WordEntity>): List<Long>

    @Query("SELECT * FROM words WHERE id = :wordId")
    suspend fun getWordById(wordId: Long): WordEntity?

    @Query("SELECT * FROM words WHERE lemma = :lemma LIMIT 1")
    suspend fun getWordByLemma(lemma: String): WordEntity?

    @Query("SELECT * FROM words WHERE id IN (:wordIds) ORDER BY lemma ASC")
    suspend fun getWordsByIds(wordIds: List<Long>): List<WordEntity>

    @Query("SELECT * FROM words ORDER BY id ASC")
    suspend fun getAllWords(): List<WordEntity>

    @Query("SELECT * FROM words ORDER BY lemma ASC")
    fun observeWords(): Flow<List<WordEntity>>

    @Query(
        """
        SELECT * FROM words
        WHERE lemma LIKE '%' || :query || '%'
        ORDER BY lemma ASC
        """,
    )
    fun searchWords(query: String): Flow<List<WordEntity>>

    @Update
    suspend fun updateWord(word: WordEntity)

    @Query("DELETE FROM words")
    suspend fun clearWords()
}
