package com.yueliangmanle.danci.feature.study

import com.yueliangmanle.danci.core.ai.AiPlanAdjustmentResult
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.StudyCardItem
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudyViewModelTest {
    @Test
    fun emitsCheckpointRequestAfterFifteenCompletedWords() {
        val viewModel = StudyViewModel(initialQueue = sampleQueue(size = 15))

        repeat(15) {
            viewModel.submitFeedback(CardFeedback.KNOWN)
        }

        val checkpoint = viewModel.consumeCheckpointRequest()

        requireNotNull(checkpoint)
        assertEquals(15, checkpoint.completedCount)
        assertEquals("已完成 15 个词，适合做阶段策略检查。", checkpoint.reason)
    }

    @Test
    fun dropsCheckpointRequestWhenSessionCheckpointsAreDisabled() {
        val viewModel = StudyViewModel(initialQueue = sampleQueue(size = 15))

        repeat(15) {
            viewModel.submitFeedback(CardFeedback.KNOWN)
        }

        assertNull(viewModel.consumeCheckpointRequest(sessionCheckpointsEnabled = false))
        assertNull(viewModel.consumeCheckpointRequest(sessionCheckpointsEnabled = true))
    }

    @Test
    fun applyCheckpointSuggestion_marksPendingConfirmationForMajorPlan() {
        val viewModel = StudyViewModel(initialQueue = sampleQueue(size = 1))

        val state = viewModel.applyCheckpointSuggestion(
            adjustment = AiPlanAdjustmentResult(
                summary = "先暂停新词，回拉错词。",
                recommendedFocus = listOf("abandon"),
                suggestedModes = listOf("quiz", "dictation"),
                suggestedPace = "slow_down",
                checkpointAdvice = "需要先确认这次大调整。",
                source = PlanSource.AI,
            ),
            version = PlanHistoryEntry(
                id = 8L,
                generatedAt = Instant.parse("2026-03-21T12:00:00Z"),
                summary = "候选大调整",
                recommendedFocus = listOf("abandon"),
                suggestedModes = listOf("quiz", "dictation"),
                suggestedPace = "slow_down",
                severity = PlanSeverity.MAJOR,
                applyStatus = PlanApplyStatus.PENDING_CONFIRMATION,
            ),
        )

        assertEquals("需要确认", state.checkpointDecisionLabel)
        assertEquals(8L, state.checkpointPlanVersionId)
    }

    private fun sampleQueue(size: Int): List<StudyCardItem> =
        (1..size).map { index ->
            StudyCardItem(
                wordId = index.toLong(),
                word = "word$index",
                meanings = listOf("meaning$index"),
            )
        }
}
