package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.WeeklySummary
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiContextBuilderTest {
    @Test
    fun contextIncludesSummariesButExcludesApiKey() {
        val payload = AiContextBuilder().buildPlanAdjustmentContext(
            settings = AiRuntimeSettings(
                enabled = true,
                baseUrl = "https://api.example.com/v1",
                apiKey = "sk-secret",
                model = "gpt-5-mini",
            ),
            memory = sampleAiMemory(),
            activeBookTitle = "四级核心词",
            dailyGoal = 30,
            anomalyNotes = listOf("最近 10 题里错了 4 题"),
        )

        assertTrue(payload.contains("learner_profile"))
        assertTrue(payload.contains("最近 10 题里错了 4 题"))
        assertFalse(payload.contains("sk-secret"))
    }

    private fun sampleAiMemory(): AiMemorySummary =
        AiMemorySummary(
            learnerProfile = LearnerProfile(
                vocabularyLevel = "提升中",
                weakSpots = listOf("word:abandon", "word:precise"),
                preferredQuestionTypes = listOf("card", "quiz"),
                commonMistakePatterns = listOf("confused_with", "similar_spelling"),
                updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
            ),
            dailySummaries = listOf(
                DailySummary(
                    date = "2026-03-17",
                    studiedCount = 28,
                    reviewCount = 19,
                    correctRate = 0.73f,
                    fatigueNote = "后段反应变慢",
                    primaryMistakeReasons = listOf("近义词辨析"),
                    updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                ),
            ),
            weeklySummaries = listOf(
                WeeklySummary(
                    weekStartDate = "2026-03-16",
                    studiedCount = 136,
                    correctRate = 0.76f,
                    trendSummary = "本周保持推进",
                    persistentWeakSpots = listOf("拼写相近词", "近义词辨析"),
                    updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                ),
            ),
            planHistory = listOf(
                PlanHistoryEntry(
                    id = 1L,
                    generatedAt = Instant.parse("2026-03-17T10:00:00Z"),
                    summary = "先回拉错词，再加新词",
                    recommendedFocus = listOf("abandon", "precise"),
                    suggestedPace = "steady",
                    executionEffect = "次日正确率提升 8%",
                ),
            ),
        )
}
