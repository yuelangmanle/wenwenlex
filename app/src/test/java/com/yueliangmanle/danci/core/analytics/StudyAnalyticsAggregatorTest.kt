package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.data.AiMemoryRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.CheckpointSummary
import com.yueliangmanle.danci.core.model.ConfusionEdge
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.StudySession
import com.yueliangmanle.danci.core.model.WeeklySummary
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class StudyAnalyticsAggregatorTest {
    @Test
    fun aggregate_buildsFeedbackBreakdownFromNonBlankFeedback() {
        val result = StudyAnalyticsAggregator().aggregate(
            events = listOf(
                correctEvent(happenedAt = "2026-03-18T08:00:00Z"),
                mistakeEvent(happenedAt = "2026-03-18T08:10:00Z"),
                mistakeEvent(happenedAt = "2026-03-18T08:20:00Z"),
                correctEvent(happenedAt = "2026-03-18T08:30:00Z").copy(feedback = null),
            ),
            referenceTime = Instant.parse("2026-03-18T12:00:00Z"),
        )

        assertEquals(listOf("wrong", "correct"), result.feedbackBreakdown.map { it.label })
        assertEquals(listOf(2, 1), result.feedbackBreakdown.map { it.count })
        assertEquals(2f / 3f, result.feedbackBreakdown.first().ratio, 0.0001f)
        assertEquals(1f / 3f, result.feedbackBreakdown.last().ratio, 0.0001f)
    }

    @Test
    fun aggregate_buildsPronunciationUsageFromAudioPlayedEvents() {
        val result = StudyAnalyticsAggregator().aggregate(
            events = listOf(
                audioEvent(
                    happenedAt = "2026-03-18T08:00:00Z",
                    playContext = "study",
                ),
                audioEvent(
                    happenedAt = "2026-03-18T08:02:00Z",
                    playContext = "follow_read",
                ),
                audioEvent(
                    happenedAt = "2026-03-18T08:05:00Z",
                    playContext = "shadowing",
                ),
                audioEvent(
                    happenedAt = "2026-03-18T08:08:00Z",
                    playContext = "preview",
                ),
            ),
            referenceTime = Instant.parse("2026-03-18T12:00:00Z"),
        )

        assertEquals(4, result.pronunciationUsage.voicePlaybackCount)
        assertEquals(2, result.pronunciationUsage.followReadCount)
        assertEquals(1, result.pronunciationUsage.shadowingCount)
    }

    @Test
    fun aggregate_usesLocalZoneForDailyBoundaryInsteadOfUtc() {
        val result = StudyAnalyticsAggregator(
            zoneId = ZoneId.of("Asia/Shanghai"),
        ).aggregate(
            events = listOf(
                correctEvent(happenedAt = "2026-03-18T15:50:00Z"),
                correctEvent(happenedAt = "2026-03-18T16:10:00Z"),
            ),
            referenceTime = Instant.parse("2026-03-19T12:00:00Z"),
        )

        assertEquals(listOf("2026-03-18", "2026-03-19"), result.dailySummaries.map { it.date })
    }

    @Test
    fun aggregate_preferredQuestionTypes_ignoreAudioAndAiEvents() {
        val result = StudyAnalyticsAggregator().aggregate(
            events = listOf(
                answerEvent(
                    happenedAt = "2026-03-18T08:00:00Z",
                    eventType = StudyEventType.QUIZ_ANSWERED,
                    isCorrect = true,
                    metadata = studyEventMetadataOf("mode" to "quiz"),
                ),
                answerEvent(
                    happenedAt = "2026-03-18T08:05:00Z",
                    eventType = StudyEventType.QUIZ_ANSWERED,
                    isCorrect = false,
                    metadata = studyEventMetadataOf("mode" to "quiz"),
                ),
                answerEvent(
                    happenedAt = "2026-03-18T08:10:00Z",
                    eventType = StudyEventType.CARD_FEEDBACK,
                    isCorrect = true,
                    metadata = studyEventMetadataOf("mode" to "flashcard"),
                ),
                answerEvent(
                    happenedAt = "2026-03-18T08:15:00Z",
                    eventType = StudyEventType.AUDIO_PLAYED,
                    isCorrect = null,
                    metadata = studyEventMetadataOf("mode" to "shadowing"),
                ),
                answerEvent(
                    happenedAt = "2026-03-18T08:20:00Z",
                    eventType = StudyEventType.AI_ACTION,
                    isCorrect = null,
                    metadata = studyEventMetadataOf("mode" to "ai_coach"),
                ),
            ),
            referenceTime = Instant.parse("2026-03-18T12:00:00Z"),
        )

        assertEquals(listOf("quiz", "flashcard"), result.learnerProfile.preferredQuestionTypes)
    }

    @Test
    fun buildsConfusionGraphFromRepeatedMistakes() {
        val result = StudyAnalyticsAggregator().aggregate(
            events = listOf(
                mistakeEvent(happenedAt = "2026-03-18T08:00:00Z"),
                mistakeEvent(happenedAt = "2026-03-18T08:10:00Z"),
                correctEvent(happenedAt = "2026-03-18T08:20:00Z"),
            ),
            referenceTime = Instant.parse("2026-03-18T12:00:00Z"),
        )

        val edge = result.confusionEdges.single()
        assertEquals(1L, edge.sourceWordId)
        assertEquals(2L, edge.targetWordId)
        assertEquals("confused_with", edge.relationType)
        assertEquals(2, edge.mistakeCount)
        assertEquals(2f, edge.weight)
        assertFalse(result.dailySummaries.isEmpty())
    }

    @Test
    fun refreshMemorySummary_preservesPlanHistoryAndCompactedCheckpointSummaries() = runTest {
        val repository = AiMemoryRepository(
            studyRepository = FakeStudyRepository(
                recentEvents = listOf(
                    mistakeEvent(happenedAt = "2026-03-20T08:00:00Z"),
                    correctEvent(happenedAt = "2026-03-20T08:15:00Z"),
                ),
                initialSummary = AiMemorySummary(
                    planHistory = listOf(
                        PlanHistoryEntry(
                            id = 7L,
                            generatedAt = Instant.parse("2026-03-20T07:30:00Z"),
                            summary = "先回拉易混词",
                        ),
                    ),
                    checkpointSummaries = listOf(
                        checkpointSummary(
                            checkpointId = "checkpoint-1",
                            windowStartAt = "2026-03-01T08:00:00Z",
                            windowEndAt = "2026-03-01T08:30:00Z",
                        ),
                        checkpointSummary(
                            checkpointId = "checkpoint-2",
                            windowStartAt = "2026-03-10T08:00:00Z",
                            windowEndAt = "2026-03-10T08:30:00Z",
                        ),
                        checkpointSummary(
                            checkpointId = "checkpoint-3",
                            windowStartAt = "2026-03-18T08:00:00Z",
                            windowEndAt = "2026-03-18T08:30:00Z",
                        ),
                        checkpointSummary(
                            checkpointId = "checkpoint-4",
                            windowStartAt = "2026-03-20T08:00:00Z",
                            windowEndAt = "2026-03-20T08:30:00Z",
                        ),
                    ),
                ),
            ),
            wordRepository = FakeWordRepository(),
            builtInWordsProvider = { emptyList() },
            nowProvider = { Instant.parse("2026-03-21T12:00:00Z") },
        )

        val summary = repository.refreshMemorySummary(referenceTime = Instant.parse("2026-03-21T12:00:00Z"))

        assertEquals(1, summary.planHistory.size)
        assertEquals(3, summary.checkpointSummaries.size)
        assertEquals(
            listOf("checkpoint-2", "checkpoint-3", "checkpoint-4"),
            summary.checkpointSummaries.map(CheckpointSummary::checkpointId),
        )
        assertTrue(summary.dailySummaries.isNotEmpty())
    }

    private fun mistakeEvent(happenedAt: String): StudyEvent =
        StudyEvent(
            wordId = 1L,
            eventType = "quiz_answered",
            isCorrect = false,
            feedback = "wrong",
            happenedAt = Instant.parse(happenedAt),
            elapsedMillis = 11_000L,
            metadata = "mode=quiz&confusedWordId=2&relationType=confused_with",
        )

    private fun correctEvent(happenedAt: String): StudyEvent =
        StudyEvent(
            wordId = 1L,
            eventType = "quiz_answered",
            isCorrect = true,
            feedback = "correct",
            happenedAt = Instant.parse(happenedAt),
            elapsedMillis = 4_000L,
            metadata = "mode=quiz",
        )

    private fun audioEvent(
        happenedAt: String,
        playContext: String,
    ): StudyEvent =
        StudyEvent(
            wordId = 1L,
            eventType = StudyEventType.AUDIO_PLAYED,
            happenedAt = Instant.parse(happenedAt),
            metadata = studyEventMetadataOf("play_context" to playContext),
        )

    private fun answerEvent(
        happenedAt: String,
        eventType: String,
        isCorrect: Boolean?,
        metadata: String? = null,
    ): StudyEvent =
        StudyEvent(
            wordId = 1L,
            eventType = eventType,
            isCorrect = isCorrect,
            happenedAt = Instant.parse(happenedAt),
            metadata = metadata,
        )

    private fun checkpointSummary(
        checkpointId: String,
        windowStartAt: String,
        windowEndAt: String,
        decisionStatus: PlanApplyStatus = PlanApplyStatus.APPLIED,
    ): CheckpointSummary =
        CheckpointSummary(
            checkpointId = checkpointId,
            windowStartAt = Instant.parse(windowStartAt),
            windowEndAt = Instant.parse(windowEndAt),
            effectivePlanVersionId = 7L,
            candidatePlanVersionId = 8L,
            decisionStatus = decisionStatus,
            effectSummary = "正确率回升",
            signalSummary = "近义词误判升高",
        )

    private class FakeStudyRepository(
        private val recentEvents: List<StudyEvent>,
        initialSummary: AiMemorySummary,
    ) : StudyRepository {
        private var memorySummary: AiMemorySummary = initialSummary

        override fun observeLearningRecord(wordId: Long): Flow<LearningRecord?> = flowOf(null)

        override suspend fun getLearningRecordOrDefault(wordId: Long): LearningRecord =
            error("Not needed in this test")

        override suspend fun getLearningRecordsForWord(wordId: Long): List<LearningRecord> = emptyList()

        override suspend fun getAllLearningRecords(): List<LearningRecord> = emptyList()

        override suspend fun getAllStudySessions(): List<StudySession> = emptyList()

        override suspend fun upsertLearningRecord(record: LearningRecord) = Unit

        override suspend fun startSession(session: StudySession): Long = error("Not needed in this test")

        override suspend fun appendEvent(event: StudyEvent): Long = error("Not needed in this test")

        override suspend fun getStudyEventsSince(since: Instant): List<StudyEvent> = recentEvents

        override suspend fun getAllStudyEvents(): List<StudyEvent> = recentEvents

        override suspend fun getRecentStudyEvents(limit: Int): List<StudyEvent> = recentEvents.take(limit)

        override suspend fun loadAiMemorySummary(
            dailyLimit: Int,
            weeklyLimit: Int,
            planLimit: Int,
            confusionLimit: Int,
        ): AiMemorySummary = memorySummary

        override suspend fun saveAiMemorySummary(summary: AiMemorySummary) {
            memorySummary = summary
        }
    }

    private class FakeWordRepository : WordRepository {
        override fun observeWords(query: String): Flow<List<Word>> = flowOf(emptyList())

        override suspend fun getWord(wordId: Long): Word? = null

        override suspend fun getWords(wordIds: List<Long>): List<Word> = emptyList()

        override suspend fun getAllWords(): List<Word> = emptyList()

        override suspend fun insertWord(word: Word): Long = word.id

        override suspend fun updateWord(word: Word) = Unit

        override suspend fun importWords(words: List<com.yueliangmanle.danci.core.importer.ImportedWord>): List<Long> =
            emptyList()
    }
}
