package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.PlanEffectSnapshot
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType

private val PERFORMANCE_EVENT_TYPES = setOf(
    StudyEventType.QUIZ_ANSWERED,
    StudyEventType.CARD_FEEDBACK,
)

internal fun StudyEvent.isPerformanceAnswerEvent(): Boolean =
    eventType in PERFORMANCE_EVENT_TYPES && isCorrect != null

internal fun List<StudyEvent>.performanceAnswerEvents(): List<StudyEvent> =
    filter(StudyEvent::isPerformanceAnswerEvent)

class PlanEffectEvaluator(
    private val windowSize: Int = 5,
    private val minimumSampleSize: Int = 2,
) {
    fun evaluate(
        currentPlan: PlanHistoryEntry,
        events: List<StudyEvent>,
    ): PlanEffectSnapshot {
        val answerEvents = events
            .performanceAnswerEvents()
            .sortedBy(StudyEvent::happenedAt)

        val beforeWindow = answerEvents
            .filter { it.happenedAt < currentPlan.generatedAt }
            .takeLast(windowSize)
        val afterWindow = answerEvents
            .filter { it.happenedAt > currentPlan.generatedAt }
            .take(windowSize)

        val hasEnoughSamples = beforeWindow.size >= minimumSampleSize && afterWindow.size >= minimumSampleSize
        val beforeRate = beforeWindow.correctRate().takeIf { hasEnoughSamples }
        val afterRate = afterWindow.correctRate().takeIf { hasEnoughSamples }

        return PlanEffectSnapshot(
            planVersionId = currentPlan.id,
            label = currentPlan.changeSummary ?: currentPlan.summary,
            beforeCorrectRate = beforeRate,
            afterCorrectRate = afterRate,
            outcomeSummary = outcomeSummary(beforeRate, afterRate),
        )
    }

    private fun outcomeSummary(
        beforeRate: Float?,
        afterRate: Float?,
    ): String =
        when {
            beforeRate == null || afterRate == null -> "样本不足"
            afterRate - beforeRate >= 0.1f -> "正确率回升"
            beforeRate - afterRate >= 0.1f -> "正确率下滑"
            else -> "基本持平"
        }

    private fun List<StudyEvent>.correctRate(): Float =
        if (isEmpty()) {
            0f
        } else {
            count { it.isCorrect == true }.toFloat() / size
        }
}
