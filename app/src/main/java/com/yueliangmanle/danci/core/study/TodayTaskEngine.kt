package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord

data class TodayPlan(
    val rescueCount: Int,
    val newWordCount: Int,
    val reviewCount: Int,
    val estimatedMinutes: Int,
    val queueHeadline: String,
)

class TodayTaskEngine(
    private val reviewPriorityEngine: ReviewPriorityEngine = ReviewPriorityEngine(),
    private val dailyQueueComposer: DailyQueueComposer = DailyQueueComposer(),
) {
    fun build(
        dailyGoal: Int,
        learningRecords: List<LearningRecord>,
        unseenWords: Int,
        now: java.time.Instant = java.time.Instant.now(),
    ): TodayPlan =
        dailyQueueComposer.compose(
            goal = dailyGoal,
            ranked = reviewPriorityEngine.rank(learningRecords, now),
            unseenWords = unseenWords,
        )
}
