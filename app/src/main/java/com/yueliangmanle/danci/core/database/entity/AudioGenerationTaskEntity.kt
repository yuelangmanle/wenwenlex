package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "audio_generation_tasks")
data class AudioGenerationTaskEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val presetId: String? = null,
    val scopeType: String,
    val scopeRef: String,
    val status: String,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val failedItems: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
)
