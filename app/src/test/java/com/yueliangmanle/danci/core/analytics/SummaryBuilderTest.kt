package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.AnalyticsOverview
import com.yueliangmanle.danci.core.model.CheckpointSummary
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.DailyTrendPoint
import com.yueliangmanle.danci.core.model.FeedbackBucket
import com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanEffectSnapshot
import com.yueliangmanle.danci.core.model.PronunciationUsageSnapshot
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.WeeklySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryBuilderTest {
    @Test
    fun buildsSevenDayAndThirtyDaySlicesWithoutRawLogFlooding() {
        val payload = SummaryBuilder().buildContext(
            sevenDay = sampleDailySummaries(count = 7),
            thirtyDay = sampleWeeklySummaries(count = 4),
            rawEvents = sampleEvents(count = 500),
        )

        assertEquals(7, payload.dailyTrend.size)
        assertEquals(4, payload.weeklyTrend.size)
        assertTrue(payload.rawSamples.size <= 20)
        assertTrue(payload.dailyTrend.isNotEmpty())
    }

    @Test
    fun buildContext_includesAnalyticsSnapshotLongTermInsightsAndPlanEffects() {
        val payload = SummaryBuilder().buildContext(
            sevenDay = sampleDailySummaries(count = 7),
            thirtyDay = sampleWeeklySummaries(count = 4),
            rawEvents = sampleEvents(count = 40),
            analyticsSnapshot = sampleAnalyticsSnapshot(),
        )

        assertEquals(1, payload.analyticsSnapshot?.planEffects?.size)
        assertEquals(1, payload.planEffects.size)
        assertEquals("正确率回升", payload.planEffects.single().outcomeSummary)
        assertTrue(payload.longTermInsights.isNotEmpty())
        assertTrue(payload.longTermInsights.any { it.contains("发音") })
        assertTrue(payload.longTermInsights.any { it.contains("调整") || it.contains("正确率回升") })
    }

    @Test
    fun buildContext_prefersLatestCheckpointWhenItPointsToNewerCandidatePlan() {
        val payload = SummaryBuilder().buildContext(
            sevenDay = sampleDailySummaries(count = 7),
            thirtyDay = sampleWeeklySummaries(count = 4),
            rawEvents = sampleEvents(count = 40),
            analyticsSnapshot = sampleAnalyticsSnapshot(),
            checkpointSummaries = listOf(
                CheckpointSummary(
                    checkpointId = "checkpoint-newer",
                    windowStartAt = Instant.parse("2026-03-20T08:00:00Z"),
                    windowEndAt = Instant.parse("2026-03-20T08:30:00Z"),
                    effectivePlanVersionId = 7L,
                    candidatePlanVersionId = 9L,
                    decisionStatus = PlanApplyStatus.PENDING_CONFIRMATION,
                    effectSummary = "当前还在观察更激进的回拉方案",
                    signalSummary = "最近 20 题里近义词误判明显升高",
                ),
            ),
        )

        assertTrue(payload.longTermInsights.any { it.contains("最近检查点显示") })
        assertFalse(payload.longTermInsights.any { it.contains("先回拉易混词，再恢复推进") })
    }

    private fun sampleDailySummaries(count: Int): List<DailySummary> =
        List(count) { index ->
            val date = LocalDate.parse("2026-03-12").plusDays(index.toLong())
            DailySummary(
                date = date.toString(),
                studiedCount = 20 + index,
                reviewCount = 10 + index,
                correctRate = 0.65f + (index * 0.02f),
                fatigueNote = if (index >= 5) "后段反应变慢" else null,
                primaryMistakeReasons = listOf("易混义项"),
                updatedAt = date.atStartOfDay().toInstant(ZoneOffset.UTC),
            )
        }

    private fun sampleWeeklySummaries(count: Int): List<WeeklySummary> =
        List(count) { index ->
            val weekStart = LocalDate.parse("2026-02-16").plusWeeks(index.toLong())
            WeeklySummary(
                weekStartDate = weekStart.toString(),
                studiedCount = 120 + (index * 8),
                correctRate = 0.6f + (index * 0.05f),
                trendSummary = "第 ${index + 1} 周保持推进",
                persistentWeakSpots = listOf("近义词辨析", "拼写相近词"),
                updatedAt = weekStart.atStartOfDay().toInstant(ZoneOffset.UTC),
            )
        }

    private fun sampleEvents(count: Int): List<StudyEvent> =
        List(count) { index ->
            StudyEvent(
                id = index.toLong() + 1L,
                wordId = (index % 20).toLong() + 1L,
                eventType = if (index % 3 == 0) "quiz_answered" else "card_feedback",
                feedback = if (index % 4 == 0) "wrong" else "known",
                isCorrect = index % 4 != 0,
                happenedAt = Instant.parse("2026-03-18T12:00:00Z").minusSeconds(index.toLong() * 90L),
                elapsedMillis = 2_000L + (index % 8) * 1_000L,
                metadata = if (index % 4 == 0) {
                    "mode=quiz&confusedWordId=${(index % 7) + 2}&relationType=confused_with"
                } else {
                    "mode=card"
                },
            )
        }

    private fun sampleAnalyticsSnapshot(): LearningAnalyticsSnapshot =
        LearningAnalyticsSnapshot(
            overview = AnalyticsOverview(
                accuracyRate = 0.78f,
                studiedDays = 7,
                masteredCount = 42,
            ),
            dailyTrend = listOf(
                DailyTrendPoint(
                    date = "2026-03-18",
                    studiedCount = 28,
                    correctRate = 0.8f,
                ),
            ),
            feedbackBreakdown = listOf(
                FeedbackBucket(
                    label = "wrong",
                    count = 6,
                    ratio = 0.4f,
                ),
            ),
            planEffects = listOf(
                PlanEffectSnapshot(
                    planVersionId = 7L,
                    label = "先回拉易混词，再恢复推进",
                    beforeCorrectRate = 0.62f,
                    afterCorrectRate = 0.78f,
                    outcomeSummary = "正确率回升",
                ),
            ),
            pronunciationUsage = PronunciationUsageSnapshot(
                followReadCount = 5,
                voicePlaybackCount = 9,
                shadowingCount = 2,
            ),
        )
}
