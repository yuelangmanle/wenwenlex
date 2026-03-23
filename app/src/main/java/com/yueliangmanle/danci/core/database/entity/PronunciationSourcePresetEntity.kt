package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "pronunciation_source_presets",
    primaryKeys = ["sourceId", "presetId"],
)
data class PronunciationSourcePresetEntity(
    val sourceId: String,
    val presetId: String,
    val displayName: String,
    val voice: String,
    val styleTemplate: String? = null,
    val advancedStyleEnabled: Boolean = false,
    val isDefaultPreset: Boolean = false,
)
