package com.yueliangmanle.danci.core.study

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTaskEngineTest {
    @Test
    fun keepsRemainingNewWordsIndependentFromReviewAndMistakeBuckets() {
        val plan = TodayTaskEngine().build(
            remainingNewWords = 19,
            overdueWords = 12,
            recentMistakeWords = 4,
        )

        assertEquals(19, plan.newWordCount)
        assertEquals(12, plan.reviewCount)
        assertEquals(4, plan.mistakeCount)
        assertEquals(25, plan.estimatedMinutes)
    }
}
