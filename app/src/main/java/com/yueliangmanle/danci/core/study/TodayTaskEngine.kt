package com.yueliangmanle.danci.core.study

import kotlin.math.min
import kotlin.math.roundToInt

data class TodayPlan(
    val newWordCount: Int,
    val reviewCount: Int,
    val mistakeCount: Int,
    val estimatedMinutes: Int,
)

class TodayTaskEngine {
    fun build(
        dailyGoal: Int,
        overdueWords: Int,
        unseenWords: Int,
        recentMistakeWords: Int,
    ): TodayPlan {
        val safeDailyGoal = dailyGoal.coerceAtLeast(0)
        val review = min(overdueWords.coerceAtLeast(0), safeDailyGoal)
        val remaining = (safeDailyGoal - review).coerceAtLeast(0)
        val newWords = min(unseenWords.coerceAtLeast(0), remaining)
        val mistakes = recentMistakeWords.coerceAtLeast(0)
        val totalItems = review + newWords + mistakes

        return TodayPlan(
            newWordCount = newWords,
            reviewCount = review,
            mistakeCount = mistakes,
            estimatedMinutes = if (totalItems == 0) {
                0
            } else {
                (totalItems * 0.7f).roundToInt()
            },
        )
    }
}
