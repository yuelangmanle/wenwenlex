package com.yueliangmanle.danci.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DanciDatabaseTest {

    private var database: DanciDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        database = null
    }

    @Test
    fun wordAppearingInTwoBooksSharesOneLearningRecord() = runTest {
        val db = buildTestDatabase().also { database = it }
        val wordId = db.wordDao().insertWord(sampleWordEntity())
        db.bookDao().insertBook(sampleBookEntity(id = "cet4", title = "四级核心词"))
        db.bookDao().insertBook(sampleBookEntity(id = "kaoyan", title = "考研核心词"))
        db.bookDao().insertBookWordCrossRef(BookWordEntity(bookId = "cet4", wordId = wordId))
        db.bookDao().insertBookWordCrossRef(BookWordEntity(bookId = "kaoyan", wordId = wordId))
        db.studyDao().upsertLearningRecord(
            LearningRecordEntity(
                wordId = wordId,
                mastery = 0.4f,
                familiarityState = "LEARNING",
                nextReviewAt = Instant.parse("2026-03-19T08:00:00Z"),
                reviewCount = 1,
                lapseCount = 0,
                lastReviewedAt = Instant.parse("2026-03-18T08:00:00Z"),
                lastOutcome = "hard",
            ),
        )

        val records = db.studyDao().getLearningRecordsForWord(wordId)

        assertEquals(1, records.size)
    }

    private fun buildTestDatabase(): DanciDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DanciDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()

    private fun sampleWordEntity(): WordEntity =
        WordEntity(
            lemma = "abandon",
            phonetic = "/əˈbændən/",
            meanings = listOf("放弃", "遗弃"),
            synonyms = listOf("give up", "quit"),
            antonyms = listOf("continue"),
            similarWords = listOf("abundant", "absorb"),
            wordForms = listOf("abandoned", "abandoning", "abandonment"),
            root = "bandon",
            exampleSentence = "He had to abandon the plan.",
            exampleTranslation = "他不得不放弃这个计划。",
        )

    private fun sampleBookEntity(id: String, title: String): BookEntity =
        BookEntity(
            id = id,
            title = title,
            description = "测试词书",
            language = "en",
            category = "exam",
            sourceType = "builtin",
            wordCount = 1,
            createdAt = Instant.parse("2026-03-18T00:00:00Z"),
            updatedAt = Instant.parse("2026-03-18T00:00:00Z"),
        )
}
