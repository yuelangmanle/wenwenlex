package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "confusion_edges",
    primaryKeys = ["sourceWordId", "targetWordId", "relationType"],
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceWordId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetWordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("targetWordId")],
)
data class ConfusionEdgeEntity(
    val sourceWordId: Long,
    val targetWordId: Long,
    val relationType: String,
    val weight: Float = 0f,
    val mistakeCount: Int = 0,
    val updatedAt: Instant = Instant.EPOCH,
)
