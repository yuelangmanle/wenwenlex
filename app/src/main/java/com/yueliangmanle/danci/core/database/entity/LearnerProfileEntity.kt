package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yueliangmanle.danci.core.model.LearnerProfile
import java.time.Instant

@Entity(tableName = "learner_profiles")
data class LearnerProfileEntity(
    @PrimaryKey val profileId: String = LearnerProfile.DEFAULT_PROFILE_ID,
    val vocabularyLevel: String? = null,
    val weakSpots: List<String> = emptyList(),
    val preferredQuestionTypes: List<String> = emptyList(),
    val commonMistakePatterns: List<String> = emptyList(),
    val checkpointSummariesJson: String = "[]",
    val updatedAt: Instant = Instant.EPOCH,
)
