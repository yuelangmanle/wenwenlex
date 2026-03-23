package com.yueliangmanle.danci.feature.study

import com.yueliangmanle.danci.core.ai.AiPlanAdjustmentResult
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.data.LearningRecordRecorder
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import com.yueliangmanle.danci.core.model.PronunciationSessionPreference
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventMetadataKey
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.metadataEntries
import com.yueliangmanle.danci.core.pronunciation.PronunciationSourceRegistry
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.StudyCardItem
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun skipCurrentCard_marks_deferred_end_when_no_other_card_can_be_shown() {
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
        assertEquals("本轮暂时结束", state.currentWord)
        assertEquals("0 / 1", state.progressText)
        assertTrue(state.meanings.single().contains("最后一张已暂时跳过"))
        assertEquals("true", skipEvent.metadataEntries()[StudyEventMetadataKey.SKIPPED])
        assertEquals("false", skipEvent.metadataEntries()["requeued"])
    }

    @Test
    fun switchSessionPronunciationSource_updatesSelectionLabel() = kotlinx.coroutines.test.runTest {
        val settingsRepository = StudySettingsRepository()
        val sourceRepository = StudySourceRepository(
            mutableListOf(
                studySource(
                    id = "dictionary-uk",
                    type = PronunciationSourceType.DICTIONARY,
                    isDefaultForWord = true,
                ),
                studySource(
                    id = "native-us",
                    type = PronunciationSourceType.LOCAL_NATIVE,
                    accent = "us",
                ),
            ),
        )
        val viewModel = StudyViewModel(
            initialQueue = sampleQueue(size = 1),
            settingsRepository = settingsRepository,
            pronunciationSourceRegistry = PronunciationSourceRegistry(
                sourceRepository = sourceRepository,
                voicePackRepository = StudyVoicePackRepository(),
                settingsRepository = settingsRepository,
            ),
        )

        viewModel.refreshPronunciationSourceState()
        val state = viewModel.switchSessionPronunciationSource("native-us")

        assertEquals("native-us", state.selectedPronunciationSourceId)
        assertEquals("native-us", settingsRepository.getPronunciationSessionPreference().sessionWordPronunciationSourceId)
        assertEquals("native-us", state.availablePronunciationSources.single { it.isSelected }.id)
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

private class StudySettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(com.yueliangmanle.danci.core.data.AppSettings())
    private var sessionPreference = PronunciationSessionPreference()

    override val settings: Flow<com.yueliangmanle.danci.core.data.AppSettings> = state

    override suspend fun getSettings(): com.yueliangmanle.danci.core.data.AppSettings = state.value

    override suspend fun getPronunciationSessionPreference(): PronunciationSessionPreference = sessionPreference

    override suspend fun updateSessionWordPronunciationSourceId(sourceId: String?) {
        sessionPreference = sessionPreference.copy(sessionWordPronunciationSourceId = sourceId)
    }

    override suspend fun updateSessionLongTextPronunciationSourceId(sourceId: String?) {
        sessionPreference = sessionPreference.copy(sessionLongTextPronunciationSourceId = sourceId)
    }

    override suspend fun updateDailyGoal(dailyGoal: Int) = Unit
    override suspend fun updateWeeklyGoal(weeklyGoal: Int) = Unit
    override suspend fun updatePhaseName(phaseName: String?) = Unit
    override suspend fun updatePhaseTargetWords(phaseTargetWords: Int) = Unit
    override suspend fun updateActiveBookId(bookId: String?) = Unit
    override suspend fun updateAiEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiBaseUrl(baseUrl: String) = Unit
    override suspend fun updateAiModel(model: String) = Unit
    override suspend fun updateDefaultAiProfileId(profileId: String?) = Unit
    override suspend fun updateWordHelpProfileId(profileId: String?) = Unit
    override suspend fun updatePlanAdjustmentProfileId(profileId: String?) = Unit
    override suspend fun updatePhoneticFillProfileId(profileId: String?) = Unit
    override suspend fun updateAiPlanAdjustmentEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiSessionCheckpointEnabled(enabled: Boolean) = Unit
    override suspend fun updatePreferredPronunciationAccent(accent: String) = Unit
    override suspend fun updatePronunciationMode(mode: String) = Unit
    override suspend fun updateAllowCellularVoicePackDownload(enabled: Boolean) = Unit
    override suspend fun updateAutoCacheWordAudio(enabled: Boolean) = Unit
    override suspend fun updateAudioCacheLimitMb(limitMb: Int) = Unit
    override suspend fun updateActiveVoicePackId(voicePackId: String?) = Unit
    override suspend fun updateFallbackToSystemTts(enabled: Boolean) = Unit
    override suspend fun updatePreferOfflineForLongText(enabled: Boolean) = Unit
    override suspend fun updateReminderEnabled(enabled: Boolean) = Unit
    override suspend fun updateReminderTime(hour: Int, minute: Int) = Unit
}

private class StudySourceRepository(
    private val sources: MutableList<PronunciationSource>,
) : com.yueliangmanle.danci.core.data.PronunciationSourceRepository {
    override suspend fun getAllSources(): List<PronunciationSource> = sources
    override suspend fun getSource(sourceId: String): PronunciationSource? = sources.firstOrNull { it.id == sourceId }
    override suspend fun upsertSources(sources: List<PronunciationSource>) = Unit
    override suspend fun setDefaultWordSource(sourceId: String) = Unit
    override suspend fun setDefaultLongTextSource(sourceId: String) = Unit
    override suspend fun clearAll() = Unit
}

private class StudyVoicePackRepository : com.yueliangmanle.danci.core.data.VoicePackRepository {
    override suspend fun getAllVoicePacks(): List<com.yueliangmanle.danci.core.model.VoicePack> = emptyList()
    override suspend fun getVoicePack(id: String): com.yueliangmanle.danci.core.model.VoicePack? = null
    override suspend fun activateVoicePack(id: String) = Unit
    override suspend fun upsertVoicePack(voicePack: com.yueliangmanle.danci.core.model.VoicePack) = Unit
    override suspend fun removeVoicePack(id: String) = Unit
    override suspend fun syncManifest(jsonText: String): Int = 0
    override suspend fun refreshCatalog(): Int = 0
    override suspend fun updateVoicePackStatus(id: String, status: String, installDir: String?, installedSizeBytes: Long?) = Unit
    override suspend fun markInstalled(id: String, installDir: String, installedSizeBytes: Long) = Unit
    override fun voicePackRootDir(): java.io.File = java.io.File("/tmp")
}

private fun studySource(
    id: String,
    type: PronunciationSourceType,
    accent: String = "uk",
    isDefaultForWord: Boolean = false,
): PronunciationSource =
    PronunciationSource(
        id = id,
        name = id,
        sourceType = type.storageValue,
        accent = accent,
        enabled = true,
        isDefaultForWord = isDefaultForWord,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
