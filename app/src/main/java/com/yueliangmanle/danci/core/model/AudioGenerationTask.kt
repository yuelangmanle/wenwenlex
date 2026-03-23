package com.yueliangmanle.danci.core.model

import java.time.Instant

data class AudioGenerationTask(
    val id: String,
    val sourceId: String,
    val presetId: String? = null,
    val scopeType: String,
    val scopeRef: String,
    val status: String,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val failedItems: Int = 0,
    val items: List<AudioGenerationTaskItem> = emptyList(),
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
)

data class AudioGenerationTaskItem(
    val taskId: String,
    val itemKey: String,
    val wordId: Long? = null,
    val text: String,
    val status: String,
    val failureReason: String? = null,
    val attemptCount: Int = 0,
    val generatedAssetId: Long? = null,
)
