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

    @Test
    fun applyCardFeedback_tracks_latency_and_consecutive_mistakes() {
        val updated = FeedbackMapper().applyCardFeedback(
            current = sampleRecord(
                mastery = 0.42f,
                reviewCount = 2,
                averageResponseLatencyMs = 3_000L,
                consecutiveMistakeCount = 0,
            ),
            feedback = CardFeedback.NOT_KNOWN,
            answeredAt = Instant.parse("2026-03-18T09:00:00Z"),
            responseLatencyMs = 4_200L,
        )

        assertEquals(4_200L, updated.lastResponseLatencyMs)
        assertEquals(1, updated.consecutiveMistakeCount)
        assertEquals("wrong", updated.lastOutcome)
        assertEquals(Instant.parse("2026-03-18T09:00:00Z"), updated.lastMistakeAt)
    }

    @Test
    fun applyCardFeedback_resets_mistake_streak_after_known_feedback() {
        val updated = FeedbackMapper().applyCardFeedback(
            current = sampleRecord(
                mastery = 0.68f,
                reviewCount = 3,
                averageResponseLatencyMs = 3_000L,
                consecutiveMistakeCount = 2,
                lastOutcome = "wrong",
            ),
            feedback = CardFeedback.KNOWN,
            answeredAt = Instant.parse("2026-03-18T09:00:00Z"),
            responseLatencyMs = 1_800L,
        )

        assertEquals(1_800L, updated.lastResponseLatencyMs)
        assertEquals(0, updated.consecutiveMistakeCount)
        assertEquals("correct", updated.lastOutcome)
    }

    private fun sampleRecord(
        mastery: Float,
        reviewCount: Int = 1,
        averageResponseLatencyMs: Long? = null,
        consecutiveMistakeCount: Int = 0,
        lastOutcome: String = "correct",
    ): LearningRecord =
        LearningRecord(
            wordId = 1L,
            mastery = mastery,
            familiarityState = "学习中",
            reviewCount = reviewCount,
            consecutiveCorrectCount = 1,
            lastReviewedAt = Instant.parse("2026-03-17T09:00:00Z"),
            lastOutcome = lastOutcome,
            averageResponseLatencyMs = averageResponseLatencyMs,
            consecutiveMistakeCount = consecutiveMistakeCount,
        )
}
