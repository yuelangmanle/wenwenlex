package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "audio_generation_jobs")
data class AudioGenerationJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobType: String,
    val sourceType: String,
    val scopeType: String,
    val scopeRef: String,
    val status: String,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val lastError: String? = null,
)
