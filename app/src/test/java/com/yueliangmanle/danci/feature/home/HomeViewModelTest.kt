package com.yueliangmanle.danci.feature.home

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.GoalProgressSnapshot
import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun buildUiState_usesRecentMistakeWords_forMistakeCount() {
        val now = Instant.parse("2099-03-22T08:00:00Z")
        val viewModel = HomeViewModel(
            settings = AppSettings(dailyGoal = 10),
            books = listOf(Book(id = "core", title = "核心词书", wordCount = 100)),
            learningRecords = listOf(
                learningRecord(
                    wordId = 1L,
                    nextReviewAt = now.plus(2, ChronoUnit.DAYS),
                    lastReviewedAt = now.minus(2, ChronoUnit.HOURS),
                    lastOutcome = "wrong",
                    consecutiveMistakeCount = 1,
                    forgettingRiskScore = 0.3f,
                    reviewPriorityScore = 0.25f,
                ),
            ),
            aiMemorySummary = AiMemorySummary(),
            nowProvider = { now },
        )

        val state = viewModel.buildUiState()

        assertEquals(1, state.mistakeCount)
    }

    @Test
    fun buildUiState_usesSameNowForSummaryAndTodayPlan() {
        val now = Instant.parse("2099-03-22T08:00:00Z")
        val viewModel = HomeViewModel(
            settings = AppSettings(dailyGoal = 10),
            books = listOf(Book(id = "core", title = "核心词书", wordCount = 100)),
            learningRecords = listOf(
                learningRecord(
                    wordId = 1L,
                    nextReviewAt = now.minus(1, ChronoUnit.HOURS),
                    lastReviewedAt = now.minus(2, ChronoUnit.DAYS),
                    lastOutcome = "correct",
                    forgettingRiskScore = 0.4f,
                    reviewPriorityScore = 0.45f,
                ),
            ),
            aiMemorySummary = AiMemorySummary(),
            nowProvider = { now },
        )

        val state = viewModel.buildUiState()

        assertEquals(1, state.reviewCount)
    }

    @Test
    fun buildUiState_projects_weekly_and_phase_goal_progress() {
        val now = Instant.parse("2099-03-22T08:00:00Z")
        val viewModel = HomeViewModel(
            settings = AppSettings(
                dailyGoal = 20,
                weeklyGoal = 70,
                phaseName = "六级冲刺",
                phaseTargetWords = 1200,
            ),
            books = listOf(Book(id = "core", title = "核心词书", wordCount = 100)),
            aiMemorySummary = AiMemorySummary(
                goalProgress = GoalProgressSnapshot(
                    currentDayCompletedCount = 8,
                    currentWeekCompletedCount = 36,
                    currentStreakDays = 5,
                    phaseName = "六级冲刺",
                    phaseTargetWords = 1200,
                    phaseCompletedWords = 480,
                ),
            ),
            nowProvider = { now },
        )

        val state = viewModel.buildUiState()

        assertEquals(8, state.completedCount)
        assertEquals("36 / 70", state.weeklyGoalValue)
        assertEquals("六级冲刺 · 480 / 1200", state.phaseSummary)
    }

    private fun learningRecord(
        wordId: Long,
        nextReviewAt: Instant? = null,
        lastReviewedAt: Instant? = null,
        lastOutcome: String? = null,
        consecutiveMistakeCount: Int = 0,
        forgettingRiskScore: Float = 0f,
        reviewPriorityScore: Float = 0f,
    ): LearningRecord =
        LearningRecord(
            wordId = wordId,
            mastery = 0.68f,
            familiarityState = "学习中",
            nextReviewAt = nextReviewAt,
            lastReviewedAt = lastReviewedAt,
            lastOutcome = lastOutcome,
            forgettingRiskScore = forgettingRiskScore,
            reviewPriorityScore = reviewPriorityScore,
            consecutiveMistakeCount = consecutiveMistakeCount,
            lastMistakeAt = lastReviewedAt,
        )
}
