package com.yueliangmanle.danci.core.model

import java.time.Instant

data class LearningRecord(
    val wordId: Long,
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
    val forgettingRiskScore: Float = 0f,
    val reviewPriorityScore: Float = 0f,
    val proficiencyBand: String = "new",
    val lastResponseLatencyMs: Long? = null,
    val averageResponseLatencyMs: Long? = null,
    val consecutiveMistakeCount: Int = 0,
    val lastMistakeAt: Instant? = null,
)
