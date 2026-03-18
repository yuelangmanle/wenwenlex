package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "weekly_summaries")
data class WeeklySummaryEntity(
    @PrimaryKey val weekStartDate: String,
    val studiedCount: Int,
    val correctRate: Float,
    val trendSummary: String? = null,
    val persistentWeakSpots: List<String> = emptyList(),
    val updatedAt: Instant = Instant.EPOCH,
)
