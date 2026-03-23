package com.yueliangmanle.danci.core.model

import java.time.Instant

data class PronunciationSource(
    val id: String,
    val name: String,
    val sourceType: String,
    val accent: String,
    val enabled: Boolean = true,
    val isDefaultForWord: Boolean = false,
    val isDefaultForLongText: Boolean = false,
    val providerProfileId: String? = null,
    val backingVoicePackId: String? = null,
    val sortOrder: Int = 0,
    val presets: List<PronunciationSourcePreset> = emptyList(),
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
)
