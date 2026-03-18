package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey val date: String,
    val studiedCount: Int,
    val reviewCount: Int,
    val correctRate: Float,
    val fatigueNote: String? = null,
    val primaryMistakeReasons: List<String> = emptyList(),
    val updatedAt: Instant = Instant.EPOCH,
)
