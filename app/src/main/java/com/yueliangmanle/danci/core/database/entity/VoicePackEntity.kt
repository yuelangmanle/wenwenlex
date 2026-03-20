package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "voice_packs")
data class VoicePackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val locale: String,
    val accent: String,
    val engineType: String,
    val version: String,
    val downloadUrl: String? = null,
    val manifestUrl: String? = null,
    val checksumsUrl: String? = null,
    val installDir: String? = null,
    val archiveChecksum: String? = null,
    val installedSizeBytes: Long = 0,
    val status: String,
    val isActive: Boolean = false,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
)
