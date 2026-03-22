package com.yueliangmanle.danci.core.model

data class GoalProgressSnapshot(
    val currentDayCompletedCount: Int = 0,
    val currentWeekCompletedCount: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val phaseName: String? = null,
    val phaseTargetWords: Int = 0,
    val phaseCompletedWords: Int = 0,
)
