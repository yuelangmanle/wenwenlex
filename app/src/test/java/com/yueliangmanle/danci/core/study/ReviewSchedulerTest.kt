package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewSchedulerTest {
    @Test
    fun summarize_includes_risk_layers_and_backlog_counts() {
        val now = Instant.parse("2026-03-22T08:00:00Z")
        val scheduler = ReviewScheduler(
            reviewPriorityEngine = ReviewPriorityEngine(nowProvider = { now }),
        )

        val summary = scheduler.summarize(
            learningRecords = listOf(
                learningRecord(
                    wordId = 1L,
                    nextReviewAt = now.minus(2, ChronoUnit.DAYS),
                    lastReviewedAt = now.minus(4, ChronoUnit.HOURS),
                    lastOutcome = "wrong",
                    forgettingRiskScore = 0.9f,
                    reviewPriorityScore = 0.88f,
                    consecutiveMistakeCount = 3,
                    proficiencyBand = "unstable",
                ),
                learningRecord(
                    wordId = 2L,
                    nextReviewAt = now.minus(6, ChronoUnit.HOURS),
                    lastReviewedAt = now.minus(2, ChronoUnit.DAYS),
                    lastOutcome = "correct",
                    forgettingRiskScore = 0.62f,
                    reviewPriorityScore = 0.58f,
                    proficiencyBand = "review",
                ),
                learningRecord(
                    wordId = 3L,
                    nextReviewAt = now.plus(1, ChronoUnit.DAYS),
                    lastReviewedAt = now.minus(1, ChronoUnit.DAYS),
                    lastOutcome = "correct",
                    forgettingRiskScore = 0.1f,
                    reviewPriorityScore = 0.08f,
                    mastery = 0.94f,
                    proficiencyBand = "mastered",
                ),
            ),
            now = now,
        )

        assertEquals(2, summary.overdueWords)
        assertEquals(1, summary.recentMistakeWords)
        assertEquals(1, summary.rescueWords)
        assertEquals(2, summary.highRiskWords)
        assertEquals(2, summary.backlogWords)
        assertEquals(1, summary.streakDays)
    }

    private fun learningRecord(
        wordId: Long,
        mastery: Float = 0.65f,
        nextReviewAt: Instant? = null,
        lastReviewedAt: Instant? = null,
        lastOutcome: String? = null,
        forgettingRiskScore: Float = 0f,
        reviewPriorityScore: Float = 0f,
        consecutiveMistakeCount: Int = 0,
        proficiencyBand: String = "review",
    ): LearningRecord =
        LearningRecord(
            wordId = wordId,
            mastery = mastery,
            familiarityState = "学习中",
            nextReviewAt = nextReviewAt,
            lastReviewedAt = lastReviewedAt,
            lastOutcome = lastOutcome,
            forgettingRiskScore = forgettingRiskScore,
            reviewPriorityScore = reviewPriorityScore,
            proficiencyBand = proficiencyBand,
            consecutiveMistakeCount = consecutiveMistakeCount,
            lastMistakeAt = lastReviewedAt,
        )
}
