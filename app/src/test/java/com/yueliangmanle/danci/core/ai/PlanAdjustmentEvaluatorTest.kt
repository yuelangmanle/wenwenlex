package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanAdjustmentEvaluatorTest {
    private val evaluator = PlanAdjustmentEvaluator()

    @Test
    fun evaluate_returnsMajorWhenTwoDimensionsChanged() {
        val result = evaluator.evaluate(
            previous = plan(
                recommendedFocus = listOf("abandon"),
                suggestedModes = listOf("card", "quiz"),
                suggestedPace = "steady",
            ),
            candidate = plan(
                recommendedFocus = listOf("precise"),
                suggestedModes = listOf("quiz", "dictation"),
                suggestedPace = "steady",
            ),
        )

        assertEquals(PlanSeverity.MAJOR, result)
    }

    @Test
    fun evaluate_returnsMinorWhenOnlyOneDimensionChanged() {
        val result = evaluator.evaluate(
            previous = plan(
                recommendedFocus = listOf("abandon"),
                suggestedModes = listOf("card", "quiz"),
                suggestedPace = "steady",
            ),
            candidate = plan(
                recommendedFocus = listOf("abandon"),
                suggestedModes = listOf("card", "quiz"),
                suggestedPace = "slow_down",
            ),
        )

        assertEquals(PlanSeverity.MINOR, result)
    }

    private fun plan(
        recommendedFocus: List<String>,
        suggestedModes: List<String>,
        suggestedPace: String?,
    ): PlanHistoryEntry =
        PlanHistoryEntry(
            generatedAt = Instant.parse("2026-03-21T10:00:00Z"),
            summary = "阶段调整",
            recommendedFocus = recommendedFocus,
            suggestedModes = suggestedModes,
            suggestedPace = suggestedPace,
        )
}
