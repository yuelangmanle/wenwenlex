package com.yueliangmanle.danci.core.study

import java.time.Instant
import java.time.temporal.ChronoUnit

enum class ReviewOutcome {
    PASS,
    FUZZY,
    FAIL,
}

data class ReviewStepResult(
    val stage: Int,
    val nextReviewAt: Instant,
)

class ReviewProgression(
    private val stageOffsetsMinutes: List<Long> = listOf(
        10L,
        24L * 60L,
        3L * 24L * 60L,
        7L * 24L * 60L,
        15L * 24L * 60L,
        30L * 24L * 60L,
    ),
) {
    fun advance(
        currentStage: Int?,
        outcome: ReviewOutcome,
        now: Instant,
    ): ReviewStepResult {
        val nextStage = when (outcome) {
            ReviewOutcome.PASS -> {
                if (currentStage == null) {
                    0
                } else {
                    (currentStage + 1).coerceAtMost(stageOffsetsMinutes.lastIndex)
                }
            }
            ReviewOutcome.FUZZY -> (currentStage ?: 0).minus(1).coerceAtLeast(0)
            ReviewOutcome.FAIL -> (currentStage ?: 0).minus(2).coerceAtLeast(0)
        }
        return ReviewStepResult(
            stage = nextStage,
            nextReviewAt = now.plus(stageOffsetsMinutes[nextStage], ChronoUnit.MINUTES),
        )
    }
}
