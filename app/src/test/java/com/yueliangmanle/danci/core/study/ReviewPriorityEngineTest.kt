package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPriorityEngineTest {
    @Test
    fun rank_prioritizes_overdue_and_repeated_mistakes_first() {
        val now = Instant.parse("2026-03-22T08:00:00Z")
        val records = listOf(
            learningRecord(
                wordId = 2L,
                nextReviewAt = now.minus(3, ChronoUnit.HOURS),
                lastReviewedAt = now.minus(12, ChronoUnit.HOURS),
                lastOutcome = "correct",
                forgettingRiskScore = 0.42f,
                reviewPriorityScore = 0.4f,
                averageResponseLatencyMs = 4_200L,
                proficiencyBand = "review",
            ),
            learningRecord(
                wordId = 3L,
                nextReviewAt = now.plus(1, ChronoUnit.DAYS),
                lastReviewedAt = now.minus(2, ChronoUnit.HOURS),
                lastOutcome = "correct",
                forgettingRiskScore = 0.08f,
                reviewPriorityScore = 0.1f,
                mastery = 0.92f,
                proficiencyBand = "mastered",
            ),
            learningRecord(
                wordId = 1L,
                nextReviewAt = now.minus(2, ChronoUnit.DAYS),
                lastReviewedAt = now.minus(6, ChronoUnit.HOURS),
                lastOutcome = "wrong",
                forgettingRiskScore = 0.9f,
                reviewPriorityScore = 0.88f,
                averageResponseLatencyMs = 4_800L,
                consecutiveMistakeCount = 3,
                confusionWeight = 0.6f,
                similarSpellingWeight = 0.4f,
                proficiencyBand = "unstable",
            ),
        )

        val ranked = ReviewPriorityEngine(nowProvider = { now }).rank(records)

        assertEquals(1L, ranked.first().wordId)
        assertEquals("rescue", ranked.first().bucket)
        assertTrue(ranked.first().priorityScore > ranked[1].priorityScore)
        assertEquals(3L, ranked.last().wordId)
        assertEquals("later", ranked.last().bucket)
    }

    @Test
    fun rank_prefers_last_latency_over_historical_average_when_both_exist() {
        val now = Instant.parse("2026-03-22T08:00:00Z")
        val ranked = ReviewPriorityEngine(nowProvider = { now }).rank(
            listOf(
                learningRecord(
                    wordId = 1L,
                    lastReviewedAt = now.minus(2, ChronoUnit.DAYS),
                    lastResponseLatencyMs = 600L,
                    averageResponseLatencyMs = 7_500L,
                ),
                learningRecord(
                    wordId = 2L,
                    lastReviewedAt = now.minus(2, ChronoUnit.DAYS),
                    lastResponseLatencyMs = 3_800L,
                    averageResponseLatencyMs = 1_000L,
                ),
            ),
        )

        assertEquals(2L, ranked.first().wordId)
        assertTrue(ranked.first().priorityScore > ranked[1].priorityScore)
    }

    private fun learningRecord(
        wordId: Long,
        mastery: Float = 0.65f,
        nextReviewAt: Instant? = null,
        lastReviewedAt: Instant? = null,
        lastOutcome: String? = null,
        forgettingRiskScore: Float = 0f,
        reviewPriorityScore: Float = 0f,
        lastResponseLatencyMs: Long? = null,
        averageResponseLatencyMs: Long? = null,
        consecutiveMistakeCount: Int = 0,
        confusionWeight: Float = 0f,
        similarSpellingWeight: Float = 0f,
        proficiencyBand: String = "review",
    ): LearningRecord =
        LearningRecord(
            wordId = wordId,
            mastery = mastery,
            familiarityState = "学习中",
            nextReviewAt = nextReviewAt,
            lastReviewedAt = lastReviewedAt,
            lastOutcome = lastOutcome,
            confusionWeight = confusionWeight,
            similarSpellingWeight = similarSpellingWeight,
            forgettingRiskScore = forgettingRiskScore,
            reviewPriorityScore = reviewPriorityScore,
            proficiencyBand = proficiencyBand,
            lastResponseLatencyMs = lastResponseLatencyMs,
            averageResponseLatencyMs = averageResponseLatencyMs,
            consecutiveMistakeCount = consecutiveMistakeCount,
            lastMistakeAt = lastReviewedAt,
        )
}
