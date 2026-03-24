package com.yueliangmanle.danci.core.model

import java.time.Instant

data class AudioGenerationJob(
    val id: Long = 0,
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
