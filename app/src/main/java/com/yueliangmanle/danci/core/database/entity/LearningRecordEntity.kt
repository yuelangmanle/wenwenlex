package com.yueliangmanle.danci.core.database.entity

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
    val reviewStage: Int = 0,
    val learningStage: String = "UNSEEN",
    val introducedAt: Instant? = null,
    val nextReviewAt: Instant? = null,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    val consecutiveCorrectCount: Int = 0,
    val lastReviewedAt: Instant? = null,
    val lastOutcome: String? = null,
    val lastMistakeAt: Instant? = null,
    val lastFuzzyAt: Instant? = null,
    val lastStudyMode: String? = null,
    val currentGroupPassState: String? = null,
    val confusionWeight: Float = 0f,
    val similarSpellingWeight: Float = 0f,
)
