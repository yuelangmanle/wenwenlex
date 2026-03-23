package com.yueliangmanle.danci.core.model

import java.time.Instant

data class StudySession(
    val id: Long = 0,
    val mode: String,
    val targetBookId: String? = null,
    val scopeType: String = "book",
    val scopeRef: String? = null,
    val groupSize: Int = 5,
    val currentGroupIndex: Int = 0,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val plannedCount: Int = 0,
    val completedCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val strategySnapshot: String? = null,
)
