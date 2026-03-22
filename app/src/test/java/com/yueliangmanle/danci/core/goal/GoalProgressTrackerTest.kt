package com.yueliangmanle.danci.core.goal

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventMetadataKey
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressTrackerTest {
    @Test
    fun build_aggregates_weekly_progress_streak_and_phase_completion() {
        val now = Instant.parse("2026-03-22T08:00:00Z")
        val tracker = GoalProgressTracker(zoneId = ZoneId.of("UTC"))
        val settings = AppSettings(
            dailyGoal = 20,
            weeklyGoal = 40,
            phaseName = "六级冲刺",
            phaseTargetWords = 1200,
        )

        val snapshot = tracker.build(
            records = listOf(
                learningRecord(1L, "2026-03-22T02:00:00Z"),
                learningRecord(2L, "2026-03-21T02:00:00Z"),
                learningRecord(3L, "2026-03-20T02:00:00Z"),
                learningRecord(4L, "2026-03-19T02:00:00Z"),
                learningRecord(5L, "2026-03-10T02:00:00Z"),
                learningRecord(6L, "2026-03-09T02:00:00Z"),
            ),
            settings = settings,
            now = now,
            studyEvents = listOf(
                feedbackEvent(1L, "2026-03-22T02:00:00Z"),
                feedbackEvent(2L, "2026-03-21T02:00:00Z"),
                feedbackEvent(3L, "2026-03-20T02:00:00Z"),
                feedbackEvent(4L, "2026-03-19T02:00:00Z"),
                skippedEvent(99L, "2026-03-22T03:00:00Z"),
            ),
        )

        assertEquals(1, snapshot.currentDayCompletedCount)
        assertEquals(4, snapshot.currentWeekCompletedCount)
        assertEquals(4, snapshot.currentStreakDays)
        assertEquals(4, snapshot.bestStreakDays)
        assertEquals("六级冲刺", snapshot.phaseName)
        assertEquals(1200, snapshot.phaseTargetWords)
        assertEquals(6, snapshot.phaseCompletedWords)
    }

    private fun learningRecord(
        wordId: Long,
        reviewedAt: String,
    ): LearningRecord =
        LearningRecord(
            wordId = wordId,
            mastery = 0.6f,
            familiarityState = "学习中",
            reviewCount = 1,
            lastReviewedAt = Instant.parse(reviewedAt),
            lastOutcome = "correct",
        )

    private fun feedbackEvent(
        wordId: Long,
        happenedAt: String,
    ): StudyEvent =
        StudyEvent(
            wordId = wordId,
            eventType = StudyEventType.CARD_FEEDBACK,
            happenedAt = Instant.parse(happenedAt),
            metadata = studyEventMetadataOf(StudyEventMetadataKey.SKIPPED to false),
        )

    private fun skippedEvent(
        wordId: Long,
        happenedAt: String,
    ): StudyEvent =
        StudyEvent(
            wordId = wordId,
            eventType = StudyEventType.CARD_FEEDBACK,
            happenedAt = Instant.parse(happenedAt),
            metadata = studyEventMetadataOf(StudyEventMetadataKey.SKIPPED to true),
        )
}
