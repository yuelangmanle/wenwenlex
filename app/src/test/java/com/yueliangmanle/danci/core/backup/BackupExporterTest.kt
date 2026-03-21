package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.AnalyticsOverview
import com.yueliangmanle.danci.core.model.BookProgressSnapshot
import com.yueliangmanle.danci.core.model.CheckpointSummary
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.DailyTrendPoint
import com.yueliangmanle.danci.core.model.FeedbackBucket
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanEffectSnapshot
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import com.yueliangmanle.danci.core.model.PronunciationUsageSnapshot
import com.yueliangmanle.danci.core.model.WeeklySummary
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupExporterTest {
    @Test
    fun backupIncludesAiMemoryButExcludesApiKey() = runTest {
        val backup = BackupExporter(
            snapshotProvider = {
                BackupSnapshot(
                    settings = AppSettings(
                        dailyGoal = 30,
                        aiEnabled = true,
                        aiBaseUrl = "https://api.openai.com/v1",
                        aiModel = "gpt-5-mini",
                    ),
                    books = listOf(
                        BookEntity(
                            id = "cet4",
                            title = "四级核心词",
                            description = "测试词书",
                            wordCount = 1,
                            createdAt = Instant.parse("2026-03-18T12:00:00Z"),
                            updatedAt = Instant.parse("2026-03-18T12:00:00Z"),
                        ),
                    ),
                    words = listOf(
                        WordEntity(
                            id = 1L,
                            lemma = "abandon",
                            meanings = listOf("放弃"),
                        ),
                    ),
                    aiMemorySummary = sampleAiMemory(),
                )
            },
            nowProvider = { Instant.parse("2026-03-18T12:30:00Z") },
        ).export()

        assertTrue(backup.manifest.sections.contains("learner_profile"))
        assertTrue(backup.manifest.sections.contains("daily_summary"))
        assertTrue(backup.manifest.sections.contains("weekly_summary"))
        assertFalse(backup.serializedJson.contains("sk-secret"))
    }

    @Test
    fun export_includesExpandedPlanHistoryAndCheckpointSummaries() = runTest {
        val backup = BackupExporter(
            snapshotProvider = {
                BackupSnapshot(
                    settings = AppSettings(),
                    aiMemorySummary = sampleAiMemory(),
                )
            },
            nowProvider = { Instant.parse("2026-03-18T12:30:00Z") },
        ).export()

        val payload = JSONObject(backup.serializedJson)
        val aiMemory = payload.getJSONObject("ai_memory_summary")

        assertEquals(5, backup.manifest.version)
        assertTrue(aiMemory.has("checkpoint_summaries"))
        assertTrue(aiMemory.has("analytics_snapshot"))
        assertTrue(aiMemory.has("long_term_insights"))
        assertTrue(aiMemory.getJSONArray("plan_history").getJSONObject(0).has("apply_status"))
        assertEquals(
            "稳住近义词误判",
            aiMemory.getJSONObject("analytics_snapshot")
                .getJSONArray("plan_effects")
                .getJSONObject(0)
                .getString("outcome_summary"),
        )
        assertEquals(
            "近义词辨析仍需复习",
            aiMemory.getJSONArray("long_term_insights").getString(0),
        )
    }

    private fun sampleAiMemory(): AiMemorySummary =
        AiMemorySummary(
            learnerProfile = LearnerProfile(
                vocabularyLevel = "提升中",
                weakSpots = listOf("abandon"),
                preferredQuestionTypes = listOf("quiz"),
                commonMistakePatterns = listOf("similar_spelling"),
                updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
            ),
            dailySummaries = listOf(
                DailySummary(
                    date = "2026-03-18",
                    studiedCount = 25,
                    reviewCount = 12,
                    correctRate = 0.76f,
                    fatigueNote = "后段变慢",
                    primaryMistakeReasons = listOf("近义词辨析"),
                    updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                ),
            ),
            weeklySummaries = listOf(
                WeeklySummary(
                    weekStartDate = "2026-03-16",
                    studiedCount = 138,
                    correctRate = 0.78f,
                    trendSummary = "本周推进稳定",
                    persistentWeakSpots = listOf("拼写相近词"),
                    updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                ),
            ),
            planHistory = listOf(
                PlanHistoryEntry(
                    id = 8L,
                    generatedAt = Instant.parse("2026-03-18T09:00:00Z"),
                    summary = "建议先回拉错词。",
                    parentPlanVersionId = 6L,
                    triggerType = "study_checkpoint",
                    sourceType = "AI",
                    recommendedFocus = listOf("abandon"),
                    suggestedModes = listOf("quiz", "dictation"),
                    suggestedPace = "slow_down",
                    reasonSummary = "最近近义词误判升高。",
                    changeSummary = "减少新词推进，增加复习比重。",
                    abnormalSignals = listOf("近义词误判升高"),
                    severity = PlanSeverity.MAJOR,
                    applyStatus = PlanApplyStatus.PENDING_CONFIRMATION,
                    executionEffect = "预计先压住错词率。",
                ),
            ),
            checkpointSummaries = listOf(
                CheckpointSummary(
                    checkpointId = "cp-20260318-1",
                    windowStartAt = Instant.parse("2026-03-18T08:00:00Z"),
                    windowEndAt = Instant.parse("2026-03-18T09:00:00Z"),
                    effectivePlanVersionId = 6L,
                    candidatePlanVersionId = 8L,
                    decisionStatus = PlanApplyStatus.PENDING_CONFIRMATION,
                    effectSummary = "错词率抬头，需要先压节奏。",
                    signalSummary = "近义词误判升高",
                ),
            ),
            analyticsSnapshot = LearningAnalyticsSnapshot(
                overview = AnalyticsOverview(
                    accuracyRate = 0.76f,
                    studiedDays = 6,
                    masteredCount = 42,
                ),
                dailyTrend = listOf(
                    DailyTrendPoint(
                        date = "2026-03-18",
                        studiedCount = 25,
                        correctRate = 0.76f,
                    ),
                ),
                feedbackBreakdown = listOf(
                    FeedbackBucket(
                        label = "近义词混淆",
                        count = 4,
                        ratio = 0.25f,
                    ),
                ),
                bookProgress = listOf(
                    BookProgressSnapshot(
                        bookId = "cet4",
                        bookName = "四级核心词",
                        completedCount = 120,
                        totalCount = 300,
                    ),
                ),
                planEffects = listOf(
                    PlanEffectSnapshot(
                        planVersionId = 8L,
                        label = "回拉错词",
                        beforeCorrectRate = 0.68f,
                        afterCorrectRate = 0.76f,
                        outcomeSummary = "稳住近义词误判",
                    ),
                ),
                pronunciationUsage = PronunciationUsageSnapshot(
                    followReadCount = 9,
                    voicePlaybackCount = 15,
                    shadowingCount = 3,
                ),
            ),
            longTermInsights = listOf("近义词辨析仍需复习"),
        )
}
