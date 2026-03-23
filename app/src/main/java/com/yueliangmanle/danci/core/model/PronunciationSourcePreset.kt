package com.yueliangmanle.danci.core.model

data class PronunciationSourcePreset(
    val sourceId: String,
    val presetId: String,
    val displayName: String,
    val voice: String,
    val styleTemplate: String? = null,
    val advancedStyleEnabled: Boolean = false,
    val isDefaultPreset: Boolean = false,
)
