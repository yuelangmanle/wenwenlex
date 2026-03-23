package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "pronunciation_sources")
data class PronunciationSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceType: String,
    val accent: String,
    val enabled: Boolean,
    val isDefaultForWord: Boolean,
    val isDefaultForLongText: Boolean,
    val providerProfileId: String? = null,
    val backingVoicePackId: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
)
