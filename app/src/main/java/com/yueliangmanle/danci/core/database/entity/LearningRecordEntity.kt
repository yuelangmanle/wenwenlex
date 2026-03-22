package com.yueliangmanle.danci.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "learning_records",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("nextReviewAt")],
)
data class LearningRecordEntity(
    @PrimaryKey val wordId: Long,
    val mastery: Float,
    val familiarityState: String,
    val nextReviewAt: Instant? = null,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    val consecutiveCorrectCount: Int = 0,
    val lastReviewedAt: Instant? = null,
    val lastOutcome: String? = null,
    val confusionWeight: Float = 0f,
    val similarSpellingWeight: Float = 0f,
    @ColumnInfo(defaultValue = "0")
    val forgettingRiskScore: Float = 0f,
    @ColumnInfo(defaultValue = "0")
    val reviewPriorityScore: Float = 0f,
    @ColumnInfo(defaultValue = "'new'")
    val proficiencyBand: String = "new",
    val lastResponseLatencyMs: Long? = null,
    val averageResponseLatencyMs: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val consecutiveMistakeCount: Int = 0,
    val lastMistakeAt: Instant? = null,
)
