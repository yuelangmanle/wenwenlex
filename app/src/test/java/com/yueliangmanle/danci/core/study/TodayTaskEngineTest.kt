package com.yueliangmanle.danci.core.study

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTaskEngineTest {
    @Test
    fun splitsTodayQueueIntoNewReviewAndMistakeBuckets() {
        val plan = TodayTaskEngine().build(
            dailyGoal = 20,
            overdueWords = 12,
            unseenWords = 50,
            recentMistakeWords = 4,
        )

        assertEquals(8, plan.newWordCount)
        assertEquals(12, plan.reviewCount)
        assertEquals(4, plan.mistakeCount)
        assertEquals(17, plan.estimatedMinutes)
    }
}
