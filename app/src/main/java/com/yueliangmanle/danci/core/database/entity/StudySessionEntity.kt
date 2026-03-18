package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val targetBookId: String? = null,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val plannedCount: Int = 0,
    val completedCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val strategySnapshot: String? = null,
)
