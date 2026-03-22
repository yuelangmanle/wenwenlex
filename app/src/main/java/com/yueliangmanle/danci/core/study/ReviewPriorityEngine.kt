package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.min

data class RankedReviewItem(
    val wordId: Long,
    val priorityScore: Float,
    val bucket: String,
)

class ReviewPriorityEngine(
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    fun rank(
        records: List<LearningRecord>,
        now: Instant = nowProvider(),
    ): List<RankedReviewItem> {
        return records
            .map { record ->
                RankedReviewItem(
                    wordId = record.wordId,
                    priorityScore = record.priorityScore(now),
                    bucket = record.bucket(now),
                )
            }
            .sortedByDescending(RankedReviewItem::priorityScore)
    }
}

private fun LearningRecord.priorityScore(now: Instant): Float {
    val overdueHours = nextReviewAt?.let { scheduledAt ->
        maxOf(0L, ChronoUnit.HOURS.between(scheduledAt, now))
    } ?: 0L
    val overdueScore = min(overdueHours.toFloat(), 72f) * 1.2f
    val mistakeScore = consecutiveMistakeCount * 18f +
        if (lastOutcome.isMistakeOutcome()) 10f else 0f
    val latencyMs = averageResponseLatencyMs ?: lastResponseLatencyMs ?: 0L
    val latencyScore = min(latencyMs / 1000f, 8f) * 2.5f
    val masteryPenalty = (1f - mastery.coerceIn(0f, 1f)) * 12f
    val proficiencyPenalty = when (proficiencyBand.lowercase()) {
        "new" -> 8f
        "unstable" -> 12f
        "review" -> 6f
        "mastered" -> 0f
        else -> 4f
    }
    val confusionScore = (confusionWeight + similarSpellingWeight) * 12f
    val riskScore = forgettingRiskScore * 20f + reviewPriorityScore * 20f
    val recentStablePenalty = if (wasReviewedRecentlyAndStable(now)) -15f else 0f
    return overdueScore +
        mistakeScore +
        latencyScore +
        masteryPenalty +
        proficiencyPenalty +
        confusionScore +
        riskScore +
        recentStablePenalty
}

private fun LearningRecord.bucket(now: Instant): String {
    val overdueHours = nextReviewAt?.let { scheduledAt ->
        maxOf(0L, ChronoUnit.HOURS.between(scheduledAt, now))
    } ?: 0L
    return when {
        consecutiveMistakeCount >= 2 -> "rescue"
        lastOutcome.isMistakeOutcome() && overdueHours >= 12 -> "rescue"
        confusionWeight >= 0.8f || similarSpellingWeight >= 0.8f -> "rescue"
        else -> "review"
    }
}

private fun LearningRecord.wasReviewedRecentlyAndStable(now: Instant): Boolean {
    val reviewedAt = lastReviewedAt ?: return false
    val withinOneDay = Duration.between(reviewedAt, now).abs() <= Duration.ofHours(24)
    return withinOneDay &&
        consecutiveMistakeCount == 0 &&
        !lastOutcome.isMistakeOutcome() &&
        mastery >= 0.85f &&
        reviewPriorityScore <= 0.2f &&
        forgettingRiskScore <= 0.2f
}

internal fun String?.isMistakeOutcome(): Boolean =
    when (this?.lowercase()) {
        "wrong", "forgot", "again" -> true
        else -> false
    }
