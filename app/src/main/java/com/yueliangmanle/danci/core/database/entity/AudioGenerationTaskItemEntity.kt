package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "audio_generation_task_items",
    primaryKeys = ["taskId", "itemKey"],
)
data class AudioGenerationTaskItemEntity(
    val taskId: String,
    val itemKey: String,
    val wordId: Long? = null,
    val text: String,
    val status: String,
    val failureReason: String? = null,
    val attemptCount: Int = 0,
    val generatedAssetId: Long? = null,
)
