package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlanExplanationComposerTest {
    @Test
    fun composeHeadlineAndReason_doNotInlineExecutionEffect() {
        val entry = PlanHistoryEntry(
            id = 8L,
            generatedAt = Instant.parse("2026-03-21T08:00:00Z"),
            summary = "先回拉易混词，再恢复推进",
            reasonSummary = "最近近义词误判升高。",
            executionEffect = "次日正确率提升 8%",
        )

        val composer = PlanExplanationComposer()

        assertEquals("先回拉易混词，再恢复推进", composer.composeHeadline(entry))
        assertEquals("最近近义词误判升高。", composer.composeReason(entry))
        assertFalse(composer.composeReason(entry).contains("次日正确率提升 8%"))
    }
}
