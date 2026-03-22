package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class CardFeedback {
    NOT_KNOWN,
    FUZZY,
    KNOWN,
}

class FeedbackMapper {
    fun applyCardFeedback(
        current: LearningRecord,
        feedback: CardFeedback,
        answeredAt: Instant,
        responseLatencyMs: Long? = null,
    ): LearningRecord =
        when (feedback) {
            CardFeedback.NOT_KNOWN -> current.copy(
                mastery = (current.mastery - 0.2f).coerceAtLeast(0f),
                familiarityState = "生疏",
                nextReviewAt = answeredAt.plus(20, ChronoUnit.MINUTES),
                reviewCount = current.reviewCount + 1,
                lapseCount = current.lapseCount + 1,
                consecutiveCorrectCount = 0,
                lastReviewedAt = answeredAt,
                lastOutcome = "wrong",
                lastResponseLatencyMs = responseLatencyMs,
                consecutiveMistakeCount = current.consecutiveMistakeCount + 1,
                lastMistakeAt = answeredAt,
            )

            CardFeedback.FUZZY -> current.copy(
                mastery = (current.mastery + 0.05f).coerceAtMost(1f),
                familiarityState = "模糊",
                nextReviewAt = answeredAt.plus(1, ChronoUnit.DAYS),
                reviewCount = current.reviewCount + 1,
                consecutiveCorrectCount = 0,
                lastReviewedAt = answeredAt,
                lastOutcome = "fuzzy",
                lastResponseLatencyMs = responseLatencyMs,
                consecutiveMistakeCount = 0,
            )

            CardFeedback.KNOWN -> current.copy(
                mastery = (current.mastery + 0.15f).coerceAtMost(1f),
                familiarityState = "熟悉",
                nextReviewAt = answeredAt.plus(2, ChronoUnit.DAYS),
                reviewCount = current.reviewCount + 1,
                consecutiveCorrectCount = current.consecutiveCorrectCount + 1,
                lastReviewedAt = answeredAt,
                lastOutcome = "correct",
                lastResponseLatencyMs = responseLatencyMs,
                consecutiveMistakeCount = 0,
            )
        }
}
