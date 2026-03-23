package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedbackMapperTest {
    @Test
    fun knownFeedbackOnUnseenWordSchedulesFirstReviewInTenMinutes() {
        val updated = FeedbackMapper().applyCardFeedback(
            current = sampleRecord(
                mastery = 0.3f,
                learningStage = "UNSEEN",
                reviewStage = 0,
                reviewCount = 0,
                consecutiveCorrectCount = 0,
                lastReviewedAt = null,
                lastOutcome = null,
            ),
            feedback = CardFeedback.KNOWN,
            answeredAt = Instant.parse("2026-03-18T09:00:00Z"),
        )

        assertEquals(Instant.parse("2026-03-18T09:10:00Z"), updated.nextReviewAt)
        assertEquals("LEARNED_PENDING_REVIEW", updated.learningStage)
        assertEquals(0, updated.reviewStage)
        assertEquals("known", updated.lastOutcome)
    }

    @Test
    fun fuzzyFeedbackRegressesOneStageAndStoresFuzzyTimestamp() {
        val answeredAt = Instant.parse("2026-03-18T09:00:00Z")

        val updated = FeedbackMapper().applyCardFeedback(
            current = sampleRecord(
                mastery = 0.7f,
                learningStage = "REVIEW_DUE",
                reviewStage = 3,
                reviewCount = 3,
                consecutiveCorrectCount = 2,
            ),
            feedback = CardFeedback.FUZZY,
            answeredAt = answeredAt,
        )

        assertEquals(2, updated.reviewStage)
        assertEquals(Instant.parse("2026-03-21T09:00:00Z"), updated.nextReviewAt)
        assertEquals("RELAPSED", updated.learningStage)
        assertEquals(answeredAt, updated.lastFuzzyAt)
        assertEquals("fuzzy", updated.currentGroupPassState)
    }

    @Test
    fun notKnownFeedbackRegressesTwoStagesAndStoresMistakeTimestamp() {
        val answeredAt = Instant.parse("2026-03-18T09:00:00Z")

        val updated = FeedbackMapper().applyCardFeedback(
            current = sampleRecord(
                mastery = 0.8f,
                learningStage = "FAMILIAR",
                reviewStage = 4,
                reviewCount = 5,
                consecutiveCorrectCount = 4,
            ),
            feedback = CardFeedback.NOT_KNOWN,
            answeredAt = answeredAt,
        )

        assertEquals(2, updated.reviewStage)
        assertEquals(Instant.parse("2026-03-21T09:00:00Z"), updated.nextReviewAt)
        assertEquals("RELAPSED", updated.learningStage)
        assertEquals(answeredAt, updated.lastMistakeAt)
        assertEquals("not_known", updated.lastOutcome)
        assertEquals("not_known", updated.currentGroupPassState)
    }

    private fun sampleRecord(
        mastery: Float,
        learningStage: String = "LEARNING",
        reviewStage: Int = 0,
        reviewCount: Int = 1,
        consecutiveCorrectCount: Int = 1,
        lastReviewedAt: Instant? = Instant.parse("2026-03-17T09:00:00Z"),
        lastOutcome: String? = "known",
    ): LearningRecord =
        LearningRecord(
            wordId = 1L,
            mastery = mastery,
            familiarityState = "学习中",
            reviewCount = reviewCount,
            consecutiveCorrectCount = consecutiveCorrectCount,
            lastReviewedAt = lastReviewedAt,
            lastOutcome = lastOutcome,
            learningStage = learningStage,
            reviewStage = reviewStage,
        )
}
