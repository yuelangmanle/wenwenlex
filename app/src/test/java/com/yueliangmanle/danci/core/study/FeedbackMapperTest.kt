package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackMapperTest {
    @Test
    fun notKnownFeedbackSchedulesSameDayRetry() {
        val updated = FeedbackMapper().applyCardFeedback(
            current = sampleRecord(mastery = 0.3f),
            feedback = CardFeedback.NOT_KNOWN,
            answeredAt = Instant.parse("2026-03-18T09:00:00Z"),
        )

        assertEquals(Instant.parse("2026-03-18T09:20:00Z"), updated.nextReviewAt)
        assertEquals("生疏", updated.familiarityState)
    }

    private fun sampleRecord(mastery: Float): LearningRecord =
        LearningRecord(
            wordId = 1L,
            mastery = mastery,
            familiarityState = "学习中",
            reviewCount = 1,
            consecutiveCorrectCount = 1,
            lastReviewedAt = Instant.parse("2026-03-17T09:00:00Z"),
            lastOutcome = "known",
        )
}
