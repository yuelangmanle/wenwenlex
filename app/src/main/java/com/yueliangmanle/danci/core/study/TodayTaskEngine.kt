package com.yueliangmanle.danci.core.study

import kotlin.math.roundToInt

data class TodayPlan(
    val newWordCount: Int,
    val reviewCount: Int,
    val mistakeCount: Int,
    val estimatedMinutes: Int,
)

class TodayTaskEngine {
    fun build(
        remainingNewWords: Int,
        overdueWords: Int,
        recentMistakeWords: Int,
    ): TodayPlan {
        val review = overdueWords.coerceAtLeast(0)
        val newWords = remainingNewWords.coerceAtLeast(0)
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
