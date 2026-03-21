package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlanEffectEvaluatorTest {
    private val evaluator = PlanEffectEvaluator()

    @Test
    fun evaluate_returnsBeforeAfterCorrectRatesForAppliedPlan() {
        val plan = planEntry(
            generatedAt = "2026-03-18T09:00:00Z",
            applyStatus = PlanApplyStatus.APPLIED,
        )

        val result = evaluator.evaluate(
            currentPlan = plan,
            events = listOf(
                answerEvent("2026-03-18T08:10:00Z", isCorrect = false),
                answerEvent("2026-03-18T08:20:00Z", isCorrect = true),
                answerEvent("2026-03-18T08:30:00Z", isCorrect = false),
                answerEvent("2026-03-18T09:10:00Z", isCorrect = true),
                answerEvent("2026-03-18T09:20:00Z", isCorrect = true),
                answerEvent("2026-03-18T09:30:00Z", isCorrect = true),
            ),
        )

        assertEquals(plan.id, result.planVersionId)
        assertEquals(plan.summary, result.label)
        assertNotNull(result.beforeCorrectRate)
        assertNotNull(result.afterCorrectRate)
        assertEquals(1f / 3f, result.beforeCorrectRate!!, 0.0001f)
        assertEquals(1f, result.afterCorrectRate!!, 0.0001f)
        assertEquals("正确率回升", result.outcomeSummary)
    }

    @Test
    fun evaluate_returnsInsufficientWhenWindowSamplesAreNotEnough() {
        val result = evaluator.evaluate(
            currentPlan = planEntry(generatedAt = "2026-03-18T09:00:00Z"),
            events = listOf(
                answerEvent("2026-03-18T08:10:00Z", isCorrect = false),
                answerEvent("2026-03-18T09:10:00Z", isCorrect = true),
            ),
        )

        assertNull(result.beforeCorrectRate)
        assertNull(result.afterCorrectRate)
        assertEquals("样本不足", result.outcomeSummary)
    }

    @Test
    fun evaluate_ignoresAudioPlayedEventsWhenCalculatingPlanEffect() {
        val plan = planEntry(generatedAt = "2026-03-18T09:00:00Z")

        val result = evaluator.evaluate(
            currentPlan = plan,
            events = listOf(
                answerEvent("2026-03-18T08:10:00Z", isCorrect = false),
                audioEvent("2026-03-18T08:15:00Z", isCorrect = true),
                answerEvent("2026-03-18T08:20:00Z", isCorrect = true),
                answerEvent("2026-03-18T08:30:00Z", isCorrect = false),
                audioEvent("2026-03-18T09:05:00Z", isCorrect = false),
                answerEvent("2026-03-18T09:10:00Z", isCorrect = true),
                answerEvent("2026-03-18T09:20:00Z", isCorrect = true),
                answerEvent("2026-03-18T09:30:00Z", isCorrect = true),
            ),
        )

        assertEquals(1f / 3f, result.beforeCorrectRate!!, 0.0001f)
        assertEquals(1f, result.afterCorrectRate!!, 0.0001f)
        assertEquals("正确率回升", result.outcomeSummary)
    }

    @Test
    fun evaluate_usesConfirmedAtAsEffectiveTimeForAppliedPlan() {
        val plan = planEntry(
            generatedAt = "2026-03-18T09:00:00Z",
            confirmedAt = "2026-03-18T09:30:00Z",
        )

        val result = evaluator.evaluate(
            currentPlan = plan,
            events = listOf(
                answerEvent("2026-03-18T08:10:00Z", isCorrect = false),
                answerEvent("2026-03-18T08:20:00Z", isCorrect = true),
                answerEvent("2026-03-18T09:10:00Z", isCorrect = false),
                answerEvent("2026-03-18T09:40:00Z", isCorrect = true),
                answerEvent("2026-03-18T09:50:00Z", isCorrect = true),
                answerEvent("2026-03-18T10:00:00Z", isCorrect = true),
            ),
        )

        assertEquals(1f / 3f, result.beforeCorrectRate!!, 0.0001f)
        assertEquals(1f, result.afterCorrectRate!!, 0.0001f)
        assertEquals("正确率回升", result.outcomeSummary)
    }

    private fun planEntry(
        generatedAt: String,
        applyStatus: PlanApplyStatus = PlanApplyStatus.APPLIED,
        confirmedAt: String? = null,
    ): PlanHistoryEntry =
        PlanHistoryEntry(
            id = 42L,
            generatedAt = Instant.parse(generatedAt),
            summary = "强化跟读纠偏",
            applyStatus = applyStatus,
            confirmedAt = confirmedAt?.let(Instant::parse),
        )

    private fun answerEvent(
        happenedAt: String,
        isCorrect: Boolean,
    ): StudyEvent =
        StudyEvent(
            wordId = 1L,
            eventType = StudyEventType.QUIZ_ANSWERED,
            isCorrect = isCorrect,
            happenedAt = Instant.parse(happenedAt),
        )

    private fun audioEvent(
        happenedAt: String,
        isCorrect: Boolean,
    ): StudyEvent =
        StudyEvent(
            wordId = 99L,
            eventType = StudyEventType.AUDIO_PLAYED,
            isCorrect = isCorrect,
            happenedAt = Instant.parse(happenedAt),
        )
}
