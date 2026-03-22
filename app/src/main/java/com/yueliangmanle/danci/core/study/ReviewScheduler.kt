package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ReviewSummary(
    val overdueWords: Int,
    val recentMistakeWords: Int,
    val rescueWords: Int,
    val highRiskWords: Int,
    val backlogWords: Int,
    val streakDays: Int,
)

class ReviewScheduler(
    private val reviewPriorityEngine: ReviewPriorityEngine = ReviewPriorityEngine(),
) {
    fun summarize(
        learningRecords: List<LearningRecord>,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ReviewSummary {
        val ranked = reviewPriorityEngine.rank(learningRecords, now)
        val overdueWords = learningRecords.count { record ->
            val nextReviewAt = record.nextReviewAt
            nextReviewAt != null && !nextReviewAt.isAfter(now)
        }
        val recentMistakeThreshold = now.minus(3, ChronoUnit.DAYS)
        val recentMistakeWords = learningRecords.count { record ->
            record.lastOutcome.isMistakeOutcome() &&
                record.lastReviewedAt?.let { !it.isBefore(recentMistakeThreshold) } == true
        }

        return ReviewSummary(
            overdueWords = overdueWords,
            recentMistakeWords = recentMistakeWords,
            rescueWords = ranked.count { it.bucket == "rescue" },
            highRiskWords = ranked.count { it.priorityScore >= 35f },
            backlogWords = ranked.count { it.bucket == "rescue" || it.priorityScore >= 25f },
            streakDays = calculateStreakDays(learningRecords, now, zoneId),
        )
    }

    private fun calculateStreakDays(
        learningRecords: List<LearningRecord>,
        now: Instant,
        zoneId: ZoneId,
    ): Int {
        val studyDays = learningRecords
            .mapNotNull(LearningRecord::lastReviewedAt)
            .map { LocalDate.ofInstant(it, zoneId) }
            .toSet()

        if (studyDays.isEmpty()) {
            return 0
        }

        var cursor = LocalDate.ofInstant(now, zoneId)
        if (!studyDays.contains(cursor) && studyDays.contains(cursor.minusDays(1))) {
            cursor = cursor.minusDays(1)
        }

        var streak = 0
        while (studyDays.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
