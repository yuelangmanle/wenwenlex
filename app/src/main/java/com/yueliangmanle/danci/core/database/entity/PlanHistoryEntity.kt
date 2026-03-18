package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "plan_history",
    indices = [Index("generatedAt")],
)
data class PlanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val generatedAt: Instant,
    val summary: String,
    val recommendedFocus: List<String> = emptyList(),
    val suggestedPace: String? = null,
    val executionEffect: String? = null,
)
