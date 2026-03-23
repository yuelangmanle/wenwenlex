package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import java.time.Instant

enum class CardFeedback {
    NOT_KNOWN,
    FUZZY,
    KNOWN,
}

class FeedbackMapper {
    private val reviewProgression = ReviewProgression()

    fun applyCardFeedback(
        current: LearningRecord,
        feedback: CardFeedback,
        answeredAt: Instant,
    ): LearningRecord {
        val nextStep = reviewProgression.advance(
            currentStage = current.reviewStageForProgression(),
            outcome = feedback.toReviewOutcome(),
            now = answeredAt,
        )
        return when (feedback) {
            CardFeedback.NOT_KNOWN -> current.copy(
                mastery = (current.mastery - 0.2f).coerceAtLeast(0f),
                familiarityState = "生疏",
                reviewStage = nextStep.stage,
                learningStage = "RELAPSED",
                introducedAt = current.introducedAt ?: answeredAt,
                nextReviewAt = nextStep.nextReviewAt,
                reviewCount = current.reviewCount + 1,
                lapseCount = current.lapseCount + 1,
                consecutiveCorrectCount = 0,
                lastReviewedAt = answeredAt,
                lastOutcome = "not_known",
                lastMistakeAt = answeredAt,
                lastFuzzyAt = current.lastFuzzyAt,
                lastStudyMode = "card",
                currentGroupPassState = "not_known",
            )

            CardFeedback.FUZZY -> current.copy(
                mastery = (current.mastery + 0.05f).coerceAtMost(1f),
                familiarityState = "模糊",
                reviewStage = nextStep.stage,
                learningStage = "RELAPSED",
                introducedAt = current.introducedAt ?: answeredAt,
                nextReviewAt = nextStep.nextReviewAt,
                reviewCount = current.reviewCount + 1,
                consecutiveCorrectCount = 0,
                lastReviewedAt = answeredAt,
                lastOutcome = "fuzzy",
                lastMistakeAt = current.lastMistakeAt,
                lastFuzzyAt = answeredAt,
                lastStudyMode = "card",
                currentGroupPassState = "fuzzy",
            )

            CardFeedback.KNOWN -> current.copy(
                mastery = (current.mastery + 0.15f).coerceAtMost(1f),
                familiarityState = "熟悉",
                reviewStage = nextStep.stage,
                learningStage = if (nextStep.stage >= 5) "FAMILIAR" else "LEARNED_PENDING_REVIEW",
                introducedAt = current.introducedAt ?: answeredAt,
                nextReviewAt = nextStep.nextReviewAt,
                reviewCount = current.reviewCount + 1,
                consecutiveCorrectCount = current.consecutiveCorrectCount + 1,
                lastReviewedAt = answeredAt,
                lastOutcome = "known",
                lastMistakeAt = current.lastMistakeAt,
                lastFuzzyAt = current.lastFuzzyAt,
                lastStudyMode = "card",
                currentGroupPassState = "passed",
            )
        }
    }

    private fun CardFeedback.toReviewOutcome(): ReviewOutcome =
        when (this) {
            CardFeedback.KNOWN -> ReviewOutcome.PASS
            CardFeedback.FUZZY -> ReviewOutcome.FUZZY
            CardFeedback.NOT_KNOWN -> ReviewOutcome.FAIL
        }

    private fun LearningRecord.reviewStageForProgression(): Int? =
        if (reviewCount == 0 && learningStage == "UNSEEN") {
            null
        } else {
            reviewStage
        }
}
