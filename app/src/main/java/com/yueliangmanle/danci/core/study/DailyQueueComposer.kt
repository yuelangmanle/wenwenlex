package com.yueliangmanle.danci.core.study

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class DailyQueueComposer {
    fun compose(
        goal: Int,
        ranked: List<RankedReviewItem>,
        unseenWords: Int,
    ): TodayPlan {
        val safeGoal = goal.coerceAtLeast(0)
        if (safeGoal == 0) {
            return TodayPlan(
                rescueCount = 0,
                reviewCount = 0,
                newWordCount = 0,
                estimatedMinutes = 0,
                queueHeadline = "今天先回稳，不再猛推新词",
            )
        }

        val rescueCandidates = ranked.filter { it.bucket == "rescue" }
        val reviewCandidates = ranked.filter { it.bucket == "review" }
        val safeUnseenWords = unseenWords.coerceAtLeast(0)

        var rescueCount = min(
            rescueCandidates.size,
            max(1, (safeGoal * 0.35f).roundToInt()),
        )
        if (rescueCandidates.size >= max(3, safeGoal / 3)) {
            rescueCount = min(
                rescueCandidates.size,
                max(rescueCount, (safeGoal * 0.45f).roundToInt()),
            )
        }
        rescueCount = rescueCount.coerceAtMost(safeGoal)

        val remainingAfterRescue = (safeGoal - rescueCount).coerceAtLeast(0)
        val heavyRescue = rescueCandidates.size >= max(3, safeGoal / 3) ||
            rescueCount >= max(3, safeGoal / 4)
        val urgentReviewCandidates = reviewCandidates.count { candidate ->
            candidate.isDueToday || candidate.isRecentMistake || candidate.priorityScore >= 45f
        }
        var reviewCount = min(
            reviewCandidates.size,
            when {
                remainingAfterRescue == 0 -> 0
                heavyRescue -> max(urgentReviewCandidates, max(1, remainingAfterRescue * 2 / 3))
                urgentReviewCandidates > 0 -> max(urgentReviewCandidates, max(1, remainingAfterRescue / 2))
                reviewCandidates.isNotEmpty() -> max(1, remainingAfterRescue / 3)
                else -> 0
            },
        )
        val remainingAfterReview = (remainingAfterRescue - reviewCount).coerceAtLeast(0)
        val newWordBudget = when {
            remainingAfterReview == 0 -> 0
            heavyRescue -> min(remainingAfterReview, max(2, safeGoal / 5))
            else -> remainingAfterReview
        }

        var newWordCount = min(safeUnseenWords, newWordBudget)
        val leftover = safeGoal - rescueCount - reviewCount - newWordCount
        if (leftover > 0) {
            val extraReview = min(reviewCandidates.size - reviewCount, leftover)
            reviewCount += extraReview
            val extraNew = min(safeUnseenWords - newWordCount, leftover - extraReview)
            newWordCount += extraNew
        }

        return TodayPlan(
            rescueCount = rescueCount,
            reviewCount = reviewCount,
            newWordCount = newWordCount,
            estimatedMinutes = (
                rescueCount * 1.1f +
                    reviewCount * 0.8f +
                    newWordCount * 0.7f
                ).roundToInt(),
            queueHeadline = if (rescueCount > 0) {
                "今天先稳住 ${rescueCount} 个高风险词"
            } else {
                "今天推进 ${newWordCount + reviewCount} 个词"
            },
        )
    }
}
