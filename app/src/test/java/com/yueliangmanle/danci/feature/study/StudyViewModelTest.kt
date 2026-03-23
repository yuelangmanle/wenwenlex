package com.yueliangmanle.danci.feature.study

import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.StudyQueueEmptyState
import com.yueliangmanle.danci.core.study.StudyCardItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StudyViewModelTest {
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

    private fun sampleQueue(size: Int): List<StudyCardItem> =
        (1..size).map { index ->
            StudyCardItem(
                wordId = index.toLong(),
                word = "word$index",
                meanings = listOf("meaning$index"),
            )
        }
}
