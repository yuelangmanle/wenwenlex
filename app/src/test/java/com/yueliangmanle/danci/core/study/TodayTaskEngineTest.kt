package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayTaskEngineTest {
    @Test
    fun build_returns_rescue_first_plan_when_backlog_is_heavy() {
        val now = Instant.parse("2026-03-22T08:00:00Z")
        val plan = TodayTaskEngine(
            reviewPriorityEngine = ReviewPriorityEngine(nowProvider = { now }),
            dailyQueueComposer = DailyQueueComposer(),
        ).build(
            dailyGoal = 20,
            learningRecords = listOf(
                learningRecord(
                    wordId = 1L,
                    nextReviewAt = now.minus(2, ChronoUnit.DAYS),
                    lastReviewedAt = now.minus(4, ChronoUnit.HOURS),
                    lastOutcome = "wrong",
                    forgettingRiskScore = 0.92f,
                    reviewPriorityScore = 0.9f,
                    consecutiveMistakeCount = 3,
                    averageResponseLatencyMs = 5_200L,
                    proficiencyBand = "unstable",
                ),
                learningRecord(
                    wordId = 2L,
                    nextReviewAt = now.minus(1, ChronoUnit.DAYS),
                    lastReviewedAt = now.minus(8, ChronoUnit.HOURS),
                    lastOutcome = "wrong",
                    forgettingRiskScore = 0.84f,
                    reviewPriorityScore = 0.8f,
                    consecutiveMistakeCount = 2,
                    averageResponseLatencyMs = 4_100L,
                    proficiencyBand = "unstable",
                ),
                learningRecord(
                    wordId = 3L,
                    nextReviewAt = now.minus(8, ChronoUnit.HOURS),
                    lastReviewedAt = now.minus(2, ChronoUnit.DAYS),
                    lastOutcome = "correct",
                    forgettingRiskScore = 0.55f,
                    reviewPriorityScore = 0.52f,
                ),
                learningRecord(
                    wordId = 4L,
                    nextReviewAt = now.minus(5, ChronoUnit.HOURS),
                    lastReviewedAt = now.minus(3, ChronoUnit.DAYS),
                    lastOutcome = "correct",
                    forgettingRiskScore = 0.4f,
                    reviewPriorityScore = 0.35f,
                ),
            ),
            unseenWords = 50,
            now = now,
        )

        assertEquals(2, plan.rescueCount)
        assertEquals("今天先稳住 2 个高风险词", plan.queueHeadline)
        assertEquals(20, plan.rescueCount + plan.reviewCount + plan.newWordCount)
        assertTrue(plan.reviewCount > 0)
        assertTrue(plan.estimatedMinutes > 0)
    }

    @Test
    fun build_excludes_low_priority_future_words_from_today_review_quota() {
        val now = Instant.parse("2026-03-22T08:00:00Z")
        val plan = TodayTaskEngine(
            reviewPriorityEngine = ReviewPriorityEngine(nowProvider = { now }),
            dailyQueueComposer = DailyQueueComposer(),
        ).build(
            dailyGoal = 12,
            learningRecords = buildList {
                repeat(3) { index ->
                    add(
                        learningRecord(
                            wordId = index + 1L,
                            nextReviewAt = now.minus((index + 1).toLong(), ChronoUnit.HOURS),
                            lastReviewedAt = now.minus(2, ChronoUnit.DAYS),
                            lastOutcome = "correct",
                            forgettingRiskScore = 0.55f,
                            reviewPriorityScore = 0.6f,
                        ),
                    )
                }
                repeat(20) { index ->
                    add(
                        learningRecord(
                            wordId = index + 101L,
                            nextReviewAt = now.plus(7, ChronoUnit.DAYS),
                            lastReviewedAt = now.minus(1, ChronoUnit.HOURS),
                            lastOutcome = "correct",
                            forgettingRiskScore = 0.05f,
                            reviewPriorityScore = 0.05f,
                            averageResponseLatencyMs = 900L,
                            proficiencyBand = "mastered",
                        ),
                    )
                }
            },
            unseenWords = 100,
            now = now,
        )

        assertEquals(3, plan.reviewCount)
        assertEquals(9, plan.newWordCount)
    }

    private fun learningRecord(
        wordId: Long,
        nextReviewAt: Instant? = null,
        lastReviewedAt: Instant? = null,
        lastOutcome: String? = null,
        forgettingRiskScore: Float = 0f,
        reviewPriorityScore: Float = 0f,
        consecutiveMistakeCount: Int = 0,
        averageResponseLatencyMs: Long? = null,
        proficiencyBand: String = "review",
    ): LearningRecord =
        LearningRecord(
            wordId = wordId,
            mastery = 0.65f,
            familiarityState = "学习中",
            nextReviewAt = nextReviewAt,
            lastReviewedAt = lastReviewedAt,
            lastOutcome = lastOutcome,
            forgettingRiskScore = forgettingRiskScore,
            reviewPriorityScore = reviewPriorityScore,
            proficiencyBand = proficiencyBand,
            averageResponseLatencyMs = averageResponseLatencyMs,
            consecutiveMistakeCount = consecutiveMistakeCount,
            lastMistakeAt = lastReviewedAt,
        )
}
