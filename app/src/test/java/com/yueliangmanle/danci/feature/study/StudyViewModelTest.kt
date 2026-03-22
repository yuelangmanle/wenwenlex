package com.yueliangmanle.danci.feature.study

import com.yueliangmanle.danci.core.ai.AiPlanAdjustmentResult
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.data.LearningRecordRecorder
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventMetadataKey
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.metadataEntries
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.StudyCardItem
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun submitFeedback_records_queue_bucket_and_latency_metadata_and_persists_record() {
        val eventRecorder = RecordingStudyEventRecorder()
        val recordRecorder = RecordingLearningRecordRecorder()
        val viewModel = StudyViewModel(
            initialQueue = listOf(
                StudyCardItem(
                    wordId = 1L,
                    word = "abandon",
                    meanings = listOf("放弃"),
                    queueBucket = "rescue",
                ),
                StudyCardItem(
                    wordId = 2L,
                    word = "ability",
                    meanings = listOf("能力"),
                    queueBucket = "review",
                ),
            ),
            eventRecorder = eventRecorder,
            learningRecordRecorder = recordRecorder,
            nowProvider = sequentialNowProvider(
                Instant.parse("2026-03-22T08:00:00Z"),
                Instant.parse("2026-03-22T08:00:05Z"),
                Instant.parse("2026-03-22T08:00:06Z"),
            ),
        )

        val state = viewModel.submitFeedback(CardFeedback.NOT_KNOWN)

        val feedbackEvent = eventRecorder.events.last { it.eventType == StudyEventType.CARD_FEEDBACK }
        val metadata = feedbackEvent.metadataEntries()
        assertEquals("rescue", metadata[StudyEventMetadataKey.QUEUE_BUCKET])
        assertEquals("5000", metadata[StudyEventMetadataKey.RESPONSE_LATENCY_MS])
        assertEquals("false", metadata[StudyEventMetadataKey.SKIPPED])
        assertEquals("daily", metadata[StudyEventMetadataKey.GOAL_SCOPE])
        assertEquals("wrong", feedbackEvent.feedback)
        assertEquals(false, feedbackEvent.isCorrect)
        assertEquals(1, recordRecorder.records.size)
        assertEquals(5_000L, recordRecorder.records.single().lastResponseLatencyMs)
        assertEquals(1, recordRecorder.records.single().consecutiveMistakeCount)
        assertEquals(2L, state.currentWordId)
    }

    @Test
    fun skipCurrentCard_records_skip_metadata_without_persisting_learning_record() {
        val eventRecorder = RecordingStudyEventRecorder()
        val recordRecorder = RecordingLearningRecordRecorder()
        val viewModel = StudyViewModel(
            initialQueue = listOf(
                StudyCardItem(
                    wordId = 1L,
                    word = "abandon",
                    meanings = listOf("放弃"),
                    queueBucket = "rescue",
                ),
                StudyCardItem(
                    wordId = 2L,
                    word = "ability",
                    meanings = listOf("能力"),
                    queueBucket = "new",
                ),
            ),
            eventRecorder = eventRecorder,
            learningRecordRecorder = recordRecorder,
            nowProvider = sequentialNowProvider(
                Instant.parse("2026-03-22T08:00:00Z"),
                Instant.parse("2026-03-22T08:00:04Z"),
                Instant.parse("2026-03-22T08:00:05Z"),
            ),
        )

        val state = viewModel.skipCurrentCard()

        val skipEvent = eventRecorder.events.last { it.eventType == StudyEventType.CARD_FEEDBACK }
        val metadata = skipEvent.metadataEntries()
        assertEquals("rescue", metadata[StudyEventMetadataKey.QUEUE_BUCKET])
        assertEquals("4000", metadata[StudyEventMetadataKey.RESPONSE_LATENCY_MS])
        assertEquals("true", metadata[StudyEventMetadataKey.SKIPPED])
        assertEquals("daily", metadata[StudyEventMetadataKey.GOAL_SCOPE])
        assertNull(skipEvent.feedback)
        assertNull(skipEvent.isCorrect)
        assertTrue(recordRecorder.records.isEmpty())
        assertEquals(2L, state.currentWordId)
    }

    @Test
    fun skipCurrentCard_finishes_session_when_no_other_card_can_be_shown() {
        val eventRecorder = RecordingStudyEventRecorder()
        val viewModel = StudyViewModel(
            initialQueue = listOf(
                StudyCardItem(
                    wordId = 1L,
                    word = "abandon",
                    meanings = listOf("放弃"),
                    queueBucket = "rescue",
                ),
            ),
            eventRecorder = eventRecorder,
            nowProvider = sequentialNowProvider(
                Instant.parse("2026-03-22T08:00:00Z"),
                Instant.parse("2026-03-22T08:00:04Z"),
            ),
        )

        val state = viewModel.skipCurrentCard()
        val skipEvent = eventRecorder.events.last { it.eventType == StudyEventType.CARD_FEEDBACK }

        assertTrue(state.isSessionComplete)
        assertEquals("true", skipEvent.metadataEntries()[StudyEventMetadataKey.SKIPPED])
        assertEquals("false", skipEvent.metadataEntries()["requeued"])
    }

    private fun sampleQueue(size: Int): List<StudyCardItem> =
        (1..size).map { index ->
            StudyCardItem(
                wordId = index.toLong(),
                word = "word$index",
                meanings = listOf("meaning$index"),
            )
        }

    private fun sequentialNowProvider(vararg instants: Instant): () -> Instant {
        val queue = ArrayDeque(instants.toList())
        val fallback = instants.last()
        return {
            if (queue.isEmpty()) {
                fallback
            } else {
                queue.removeFirst()
            }
        }
    }
}

private class RecordingStudyEventRecorder : StudyEventRecorder {
    val events = mutableListOf<StudyEvent>()

    override fun record(event: StudyEvent) {
        events += event
    }
}

private class RecordingLearningRecordRecorder : LearningRecordRecorder {
    val records = mutableListOf<LearningRecord>()

    override fun record(record: LearningRecord) {
        records += record
    }
}
