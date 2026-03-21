package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.FeedbackBucket
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PronunciationUsageSnapshot
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningDashboardComposerTest {
    private val composer = LearningDashboardComposer(
        planEffectEvaluator = PlanEffectEvaluator(),
    )

    @Test
    fun compose_buildsSnapshotFromAggregatedAnalyticsAndPlanHistory() {
        val aggregated = AggregatedAnalytics(
            dailySummaries = listOf(
                DailySummary(
                    date = "2026-03-18",
                    studiedCount = 3,
                    reviewCount = 4,
                    correctRate = 0.75f,
                    updatedAt = Instant.parse("2026-03-18T12:00:00Z"),
                ),
            ),
            feedbackBreakdown = listOf(
                FeedbackBucket(label = "wrong", count = 2, ratio = 2f / 3f),
                FeedbackBucket(label = "correct", count = 1, ratio = 1f / 3f),
            ),
            pronunciationUsage = PronunciationUsageSnapshot(
                followReadCount = 2,
                voicePlaybackCount = 4,
                shadowingCount = 1,
            ),
            events = listOf(
                answerEvent("2026-03-18T08:10:00Z", isCorrect = false),
                answerEvent("2026-03-18T08:20:00Z", isCorrect = true),
                answerEvent("2026-03-18T08:30:00Z", isCorrect = false),
                answerEvent("2026-03-18T09:10:00Z", isCorrect = true),
                answerEvent("2026-03-18T09:20:00Z", isCorrect = true),
                answerEvent("2026-03-18T09:30:00Z", isCorrect = true),
            ),
        )
        val planHistory = listOf(
            PlanHistoryEntry(
                id = 7L,
                generatedAt = Instant.parse("2026-03-18T09:00:00Z"),
                summary = "先回拉易混词",
                applyStatus = PlanApplyStatus.APPLIED,
            ),
            PlanHistoryEntry(
                id = 8L,
                generatedAt = Instant.parse("2026-03-19T09:00:00Z"),
                summary = "待确认方案",
                applyStatus = PlanApplyStatus.PENDING_CONFIRMATION,
            ),
        )

        val snapshot = composer.compose(
            aggregated = aggregated,
            planHistory = planHistory,
            activeBookTitle = "CET-4 Core",
        )

        assertEquals(aggregated.feedbackBreakdown, snapshot.feedbackBreakdown)
        assertEquals(aggregated.pronunciationUsage, snapshot.pronunciationUsage)
        assertEquals(1, snapshot.dailyTrend.size)
        assertEquals("2026-03-18", snapshot.dailyTrend.single().date)
        assertEquals(1, snapshot.planEffects.size)
        assertEquals("正确率回升", snapshot.planEffects.single().outcomeSummary)
    }

    private fun answerEvent(
        happenedAt: String,
        isCorrect: Boolean,
    ): StudyEvent =
        StudyEvent(
            wordId = 1L,
            eventType = StudyEventType.QUIZ_ANSWERED,
            isCorrect = isCorrect,
            happenedAt = Instant.parse(happenedAt),
        )
}
