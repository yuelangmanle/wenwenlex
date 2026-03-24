package com.yueliangmanle.danci.feature.study

import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.StudyQueueEmptyState
import com.yueliangmanle.danci.core.study.StudyCardItem
import com.yueliangmanle.danci.core.study.WordPassStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StudyViewModelTest {
    @Test
    fun knownFeedbackAdvancesCurrentWordToRecallBeforeMovingQueue() {
        val viewModel = StudyViewModel(initialQueue = sampleQueue(size = 2))

        val state = viewModel.submitFeedback(CardFeedback.KNOWN)

        assertEquals("word1", state.currentWord)
        assertEquals(WordPassStep.RECALL, state.passStep)
    }

    @Test
    fun completesWordOnlyAfterAllPassStepsFinish() {
        val viewModel = StudyViewModel(initialQueue = sampleQueue(size = 2))

        viewModel.submitFeedback(CardFeedback.KNOWN)
        viewModel.submitFeedback(CardFeedback.KNOWN)
        val state = viewModel.submitFeedback(CardFeedback.KNOWN)

        assertEquals("word2", state.currentWord)
        assertEquals(WordPassStep.MEANING, state.passStep)
    }

    @Test
    fun emitsGroupSummaryAfterLastWordInGroup() {
        val viewModel = StudyViewModel(initialQueue = sampleQueue(size = 2))

        repeat(6) {
            viewModel.submitFeedback(CardFeedback.KNOWN)
        }

        val state = viewModel.buildUiState()

        assertEquals("本组 2 词已完成", state.groupSummaryTitle)
        assertEquals(true, state.showContinueNextGroup)
    }

    @Test
    fun continueNextGroupTransitionsFromGroupSummaryToCompletionWhenNoPendingWordsRemain() {
        val viewModel = StudyViewModel(initialQueue = sampleQueue(size = 1))

        repeat(3) {
            viewModel.submitFeedback(CardFeedback.KNOWN)
        }

        assertEquals("本组 1 词已完成", viewModel.buildUiState().groupSummaryTitle)

        val state = viewModel.continueNextGroup()

        assertEquals(true, state.isSessionComplete)
        assertEquals(null, state.groupSummaryTitle)
    }

    @Test
    fun exposesEmptyStateInsteadOfRenderingCompletionCardForEmptyQueue() {
        val viewModel = StudyViewModel(
            initialQueue = emptyList(),
            emptyState = StudyQueueEmptyState.NO_DUE_REVIEW,
        )

        val state = viewModel.buildUiState()

        assertFalse(state.isSessionComplete)
        assertEquals(StudyQueueEmptyState.NO_DUE_REVIEW, state.emptyState)
        assertEquals("", state.currentWord)
        assertEquals("", state.progressText)
    }

    @Test
    fun emitsCheckpointRequestAfterFifteenCompletedWords() {
        val viewModel = StudyViewModel(initialQueue = sampleQueue(size = 15))

        repeat(45) {
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

        repeat(45) {
            viewModel.submitFeedback(CardFeedback.KNOWN)
        }

        assertNull(viewModel.consumeCheckpointRequest(sessionCheckpointsEnabled = false))
        assertNull(viewModel.consumeCheckpointRequest(sessionCheckpointsEnabled = true))
    }

    @Test
    fun recordsSessionIdOnPresentedAndFeedbackEvents() {
        val recordedEvents = mutableListOf<StudyEvent>()
        val viewModel = StudyViewModel(
            initialQueue = sampleQueue(size = 1),
            sessionId = 42L,
            eventRecorder = StudyEventRecorder { event ->
                recordedEvents += event
            },
        )

        viewModel.submitFeedback(CardFeedback.KNOWN)

        assertEquals(42L, recordedEvents.first().sessionId)
        assertEquals(42L, recordedEvents.last().sessionId)
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
