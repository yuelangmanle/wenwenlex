package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewSchedulerTest {
    @Test
    fun summarize_countsCardFeedbackMistakesWithinThreeDays() {
        val now = Instant.parse("2026-03-24T10:00:00Z")

        val summary = ReviewScheduler().summarize(
            learningRecords = listOf(
                record(wordId = 1, lastOutcome = "not_known", lastReviewedAt = now.minusSeconds(60)),
                record(wordId = 2, lastOutcome = "fuzzy", lastReviewedAt = now.minusSeconds(2 * 24 * 3600)),
                record(wordId = 3, lastOutcome = "known", lastReviewedAt = now.minusSeconds(60)),
                record(wordId = 4, lastOutcome = "not_known", lastReviewedAt = now.minusSeconds(5 * 24 * 3600)),
            ),
            now = now,
        )

        assertEquals(2, summary.recentMistakeWords)
    }

    private fun record(
        wordId: Long,
        lastOutcome: String,
        lastReviewedAt: Instant,
    ): LearningRecord =
        LearningRecord(
            wordId = wordId,
            mastery = 0.4f,
            familiarityState = "生疏",
            reviewCount = 1,
            lastOutcome = lastOutcome,
            lastReviewedAt = lastReviewedAt,
        )
}
