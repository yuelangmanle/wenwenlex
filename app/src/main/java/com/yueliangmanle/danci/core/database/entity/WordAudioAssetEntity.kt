package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "word_audio_assets",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("wordId"),
        Index(value = ["wordId", "accent", "sourceType"]),
    ],
)
data class WordAudioAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val sourceId: String? = null,
    val presetId: String? = null,
    val actualSourceType: String? = null,
    val namespace: String? = null,
    val assetState: String = "ready",
    val taskId: String? = null,
    val accent: String,
    val sourceType: String,
    val remoteUrl: String? = null,
    val localPath: String? = null,
    val mimeType: String? = null,
    val checksum: String? = null,
    val status: String,
    val fetchedAt: Instant? = null,
    val lastPlayedAt: Instant? = null,
    val lastError: String? = null,
    val failureCount: Int = 0,
)
