package com.yueliangmanle.danci.feature.home

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {
    private val now = Instant.parse("2026-03-24T12:00:00Z")

    @Test
    fun buildUiState_countsOnlyRemainingUnseenWordsAgainstDailyGoal() {
        val state = HomeViewModel(
            settings = settings(dailyGoal = 20),
            books = listOf(book()),
            learningRecords = listOf(
                record(wordId = 1, introducedAt = now.minusSeconds(60)),
                record(wordId = 2, introducedAt = now.minusSeconds(24 * 60 * 60)),
            ),
            nowProvider = { now },
            zoneId = ZoneOffset.UTC,
        ).buildUiState()

        assertEquals(20, state.todayGoalCount)
        assertEquals(19, state.newWordCount)
        assertEquals(1, state.completedCount)
    }

    @Test
    fun buildUiState_usesRealOverdueAndMistakeCountsFromLearningRecords() {
        val state = HomeViewModel(
            settings = settings(dailyGoal = 20),
            books = listOf(book()),
            learningRecords = listOf(
                record(wordId = 1, nextReviewAt = now.minusSeconds(60)),
                record(wordId = 2, nextReviewAt = now.plusSeconds(60)),
                record(
                    wordId = 3,
                    lastOutcome = "not_known",
                    lastReviewedAt = now.minusSeconds(60 * 60),
                ),
                record(
                    wordId = 4,
                    lastOutcome = "wrong",
                    lastReviewedAt = now.minusSeconds(4 * 24 * 60 * 60),
                ),
            ),
            nowProvider = { now },
            zoneId = ZoneOffset.UTC,
        ).buildUiState()

        assertEquals(20, state.newWordCount)
        assertEquals(1, state.reviewCount)
        assertEquals(1, state.mistakeCount)
        assertEquals("今天还要学 22 个词", state.headline)
    }

    private fun settings(
        dailyGoal: Int,
        activeBookId: String = "book-1",
    ): AppSettings =
        AppSettings(
            dailyGoal = dailyGoal,
            activeBookId = activeBookId,
        )

    private fun book(
        id: String = "book-1",
        title: String = "考研词书",
        wordCount: Int = 100,
    ): Book =
        Book(
            id = id,
            title = title,
            wordCount = wordCount,
        )

    private fun record(
        wordId: Long,
        introducedAt: Instant? = null,
        nextReviewAt: Instant? = null,
        lastOutcome: String? = null,
        lastReviewedAt: Instant? = null,
    ): LearningRecord =
        LearningRecord(
            wordId = wordId,
            mastery = 0.3f,
            familiarityState = "未学",
            introducedAt = introducedAt,
            nextReviewAt = nextReviewAt,
            lastOutcome = lastOutcome,
            lastReviewedAt = lastReviewedAt,
        )
}
