package com.yueliangmanle.danci.feature.study

import android.content.Context
import com.yueliangmanle.danci.core.ai.AiPlanAdjustmentResult
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.data.LearningRecordRecorder
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.NoOpLearningRecordRecorder
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.buildAiProfileRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildPronunciationSourceRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.data.defaultLearningRecord
import com.yueliangmanle.danci.core.data.syncBuiltInCatalogToDatabase
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.feature.pronunciation.SessionPronunciationSourceUiState
import com.yueliangmanle.danci.feature.pronunciation.buildSessionPronunciationSourceOptions
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventMetadataKey
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import com.yueliangmanle.danci.core.pronunciation.PronunciationSourceRegistry
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.FeedbackMapper
import com.yueliangmanle.danci.core.study.ReviewPriorityEngine
import com.yueliangmanle.danci.core.study.StudyCardItem
import com.yueliangmanle.danci.core.study.StudyQueueBuilder
import java.time.Duration
import java.time.Instant

data class StudyUiState(
    val sessionTitle: String = "卡片学习",
    val currentWordId: Long = 0L,
    val currentWord: String = "",
    val phonetic: String? = null,
    val meanings: List<String> = emptyList(),
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
    val progressText: String = "",
    val isSessionComplete: Boolean = false,
    val checkpointTitle: String? = null,
    val checkpointSuggestion: String? = null,
    val checkpointSourceLabel: String? = null,
    val checkpointDecisionLabel: String? = null,
    val checkpointPlanVersionId: Long? = null,
    val canOpenPlanCenter: Boolean = false,
    val selectedPronunciationSourceId: String? = null,
    val selectedPronunciationSourceLabel: String = "跟随默认来源",
    val availablePronunciationSources: List<SessionPronunciationSourceUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class SessionCheckpointRequest(
    val completedCount: Int,
    val mistakeBurst: Int,
    val reason: String,
)

class StudyViewModel(
    initialQueue: List<StudyCardItem>,
    initialRecords: Map<Long, LearningRecord> = emptyMap(),
    private val feedbackMapper: FeedbackMapper = FeedbackMapper(),
    private val eventRecorder: StudyEventRecorder = NoOpStudyEventRecorder,
    private val learningRecordRecorder: LearningRecordRecorder = NoOpLearningRecordRecorder,
    private val settingsRepository: SettingsRepository? = null,
    private val pronunciationSourceRegistry: PronunciationSourceRegistry? = null,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private val queue = initialQueue.toMutableList()
    private val records = initialQueue.associate { card ->
        card.wordId to (initialRecords[card.wordId] ?: defaultLearningRecord(card.wordId))
    }.toMutableMap()
    private var currentIndex = 0
    private var currentCardPresentedAt: Instant = Instant.EPOCH
    private var lastPresentedWordId: Long? = null
    private var completedCount = 0
    private var recentMistakeBurst = 0
    private var pendingCheckpointRequest: SessionCheckpointRequest? = null
    private var checkpointTitle: String? = null
    private var checkpointSuggestion: String? = null
    private var checkpointSourceLabel: String? = null
    private var checkpointDecisionLabel: String? = null
    private var checkpointPlanVersionId: Long? = null
    private var canOpenPlanCenter: Boolean = false
    private var deferredSessionReason: String? = null
    private var deferredCompletedCount: Int? = null
    private var selectedPronunciationSourceId: String? = null
    private var selectedPronunciationSourceLabel: String = "跟随默认来源"
    private var availablePronunciationSources: List<SessionPronunciationSourceUiState> = emptyList()

    init {
        recordCurrentCardPresentedIfNeeded()
    }

    fun buildUiState(): StudyUiState {
        val card = queue.getOrNull(currentIndex)
        if (card == null) {
            val deferredReason = deferredSessionReason
            val deferredProgress = deferredCompletedCount
            return StudyUiState(
                currentWordId = queue.lastOrNull()?.wordId ?: 0L,
                currentWord = if (deferredReason == null) "今日学习完成" else "本轮暂时结束",
                meanings = listOf(deferredReason ?: "可以回到首页继续安排下一轮复习。"),
                progressText = deferredProgress?.let { "$it / ${queue.size}" } ?: "${queue.size} / ${queue.size}",
                isSessionComplete = true,
                checkpointTitle = checkpointTitle,
                checkpointSuggestion = checkpointSuggestion,
                checkpointSourceLabel = checkpointSourceLabel,
                checkpointDecisionLabel = checkpointDecisionLabel,
                checkpointPlanVersionId = checkpointPlanVersionId,
                canOpenPlanCenter = canOpenPlanCenter,
                selectedPronunciationSourceId = selectedPronunciationSourceId,
                selectedPronunciationSourceLabel = selectedPronunciationSourceLabel,
                availablePronunciationSources = availablePronunciationSources,
            )
        }

        return StudyUiState(
            currentWordId = card.wordId,
            currentWord = card.word,
            phonetic = card.phonetic,
            meanings = card.meanings,
            exampleSentence = card.exampleSentence,
            exampleTranslation = card.exampleTranslation,
            progressText = "${currentIndex + 1} / ${queue.size}",
            checkpointTitle = checkpointTitle,
            checkpointSuggestion = checkpointSuggestion,
            checkpointSourceLabel = checkpointSourceLabel,
            checkpointDecisionLabel = checkpointDecisionLabel,
            checkpointPlanVersionId = checkpointPlanVersionId,
            canOpenPlanCenter = canOpenPlanCenter,
            selectedPronunciationSourceId = selectedPronunciationSourceId,
            selectedPronunciationSourceLabel = selectedPronunciationSourceLabel,
            availablePronunciationSources = availablePronunciationSources,
        )
    }

    suspend fun refreshPronunciationSourceState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): StudyUiState {
        val settingsRepository = settingsRepository ?: return buildUiState().copy(
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
        val sources = pronunciationSourceRegistry?.refreshBuiltinSources().orEmpty()
        val sessionPreference = settingsRepository.getPronunciationSessionPreference()
        selectedPronunciationSourceId = sessionPreference.sessionWordPronunciationSourceId
        availablePronunciationSources = buildSessionPronunciationSourceOptions(
            sources = sources,
            selectedSourceId = selectedPronunciationSourceId,
        )
        selectedPronunciationSourceLabel = availablePronunciationSources
            .firstOrNull(SessionPronunciationSourceUiState::isSelected)
            ?.title
            ?: "跟随默认来源"
        return buildUiState().copy(
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun switchSessionPronunciationSource(
        sourceId: String?,
    ): StudyUiState {
        val settingsRepository = settingsRepository ?: return buildUiState()
        pronunciationSourceRegistry?.updateSessionWordSource(sourceId)
            ?: settingsRepository.updateSessionWordPronunciationSourceId(sourceId)
        val refreshed = refreshPronunciationSourceState()
        val message = if (sourceId == null) {
            "当前学习会话已恢复跟随默认来源。"
        } else {
            "当前学习会话已切换到${refreshed.selectedPronunciationSourceLabel}。"
        }
        return refreshed.copy(
            statusMessage = message,
            errorMessage = null,
        )
    }

    fun submitFeedback(feedback: CardFeedback): StudyUiState {
        val card = queue.getOrNull(currentIndex) ?: return buildUiState()
        clearDeferredSessionState()
        val currentRecord = records.getValue(card.wordId)
        val answeredAt = nowProvider()
        val responseLatencyMs = Duration.between(currentCardPresentedAt, answeredAt).toMillis().coerceAtLeast(0L)
        val updatedRecord = feedbackMapper.applyCardFeedback(
            current = currentRecord,
            feedback = feedback,
            answeredAt = answeredAt,
            responseLatencyMs = responseLatencyMs,
        )
        records[card.wordId] = updatedRecord
        learningRecordRecorder.record(updatedRecord)
        eventRecorder.record(
            StudyEvent(
                wordId = card.wordId,
                eventType = StudyEventType.CARD_FEEDBACK,
                feedback = feedback.toEventFeedback(),
                isCorrect = feedback == CardFeedback.KNOWN,
                happenedAt = answeredAt,
                elapsedMillis = responseLatencyMs,
                metadata = cardFeedbackMetadata(
                    card = card,
                    responseLatencyMs = responseLatencyMs,
                    skipped = false,
                    requeued = feedback == CardFeedback.NOT_KNOWN,
                ),
            ),
        )

        if (feedback == CardFeedback.NOT_KNOWN) {
            queue.add(card)
        }

        completedCount += 1
        recentMistakeBurst = if (feedback == CardFeedback.KNOWN) 0 else recentMistakeBurst + 1
        pendingCheckpointRequest = when {
            completedCount % 15 == 0 -> SessionCheckpointRequest(
                completedCount = completedCount,
                mistakeBurst = recentMistakeBurst,
                reason = "已完成 $completedCount 个词，适合做阶段策略检查。",
            )
            recentMistakeBurst >= 3 -> SessionCheckpointRequest(
                completedCount = completedCount,
                mistakeBurst = recentMistakeBurst,
                reason = "连续错了 $recentMistakeBurst 次，建议立即收缩节奏。",
            )
            else -> pendingCheckpointRequest
        }

        currentIndex += 1
        if (queue.getOrNull(currentIndex) != null) {
            recordCurrentCardPresentedIfNeeded(force = true)
        }
        return buildUiState()
    }

    fun skipCurrentCard(): StudyUiState {
        val card = queue.getOrNull(currentIndex) ?: return buildUiState()
        clearDeferredSessionState()
        val skippedAt = nowProvider()
        val responseLatencyMs = Duration.between(currentCardPresentedAt, skippedAt).toMillis().coerceAtLeast(0L)
        val hasDeferredAlternative = queue
            .drop(currentIndex + 1)
            .any { queuedCard -> queuedCard.wordId != card.wordId }
        if (hasDeferredAlternative) {
            queue.add(card)
        }
        eventRecorder.record(
            StudyEvent(
                wordId = card.wordId,
                eventType = StudyEventType.CARD_FEEDBACK,
                happenedAt = skippedAt,
                elapsedMillis = responseLatencyMs,
                metadata = cardFeedbackMetadata(
                    card = card,
                    responseLatencyMs = responseLatencyMs,
                    skipped = true,
                    requeued = hasDeferredAlternative,
                ),
            ),
        )

        if (!hasDeferredAlternative) {
            deferredSessionReason = "最后一张已暂时跳过，本轮先结束，稍后会在下次会话继续。"
            deferredCompletedCount = currentIndex.coerceAtLeast(0)
            currentIndex = queue.size
            return buildUiState()
        }

        currentIndex += 1
        while (queue.getOrNull(currentIndex)?.wordId == card.wordId) {
            currentIndex += 1
        }
        if (queue.getOrNull(currentIndex) != null) {
            recordCurrentCardPresentedIfNeeded(force = true)
        } else {
            currentIndex = queue.size
        }
        return buildUiState()
    }

    fun openCurrentWordDetail() {
        val card = queue.getOrNull(currentIndex) ?: return
        eventRecorder.record(
            StudyEvent(
                wordId = card.wordId,
                eventType = StudyEventType.DETAIL_OPENED,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "card",
                    "source" to "study",
                    StudyEventMetadataKey.QUEUE_BUCKET to card.queueBucket,
                    StudyEventMetadataKey.GOAL_SCOPE to "daily",
                ),
            ),
        )
    }

    fun currentRecord(wordId: Long): LearningRecord = records.getValue(wordId)

    fun consumeCheckpointRequest(
        sessionCheckpointsEnabled: Boolean = true,
    ): SessionCheckpointRequest? {
        val request = pendingCheckpointRequest
        pendingCheckpointRequest = null
        return request?.takeIf { sessionCheckpointsEnabled }
    }

    fun applyCheckpointSuggestion(
        adjustment: AiPlanAdjustmentResult,
        version: PlanHistoryEntry,
    ): StudyUiState {
        checkpointTitle = if (adjustment.source == PlanSource.AI) {
            "AI 阶段建议"
        } else {
            "本地阶段建议"
        }
        checkpointSuggestion = listOfNotNull(adjustment.summary, adjustment.checkpointAdvice).joinToString("\n")
        checkpointSourceLabel = if (adjustment.source == PlanSource.AI) "AI 生成" else "本地兜底"
        checkpointDecisionLabel = when (version.applyStatus) {
            PlanApplyStatus.APPLIED -> "已自动微调"
            PlanApplyStatus.PENDING_CONFIRMATION -> "需要确认"
            else -> null
        }
        checkpointPlanVersionId = version.id.takeIf { it > 0 }
        canOpenPlanCenter = checkpointPlanVersionId != null
        return buildUiState()
    }

    private fun recordCurrentCardPresentedIfNeeded(force: Boolean = false) {
        val card = queue.getOrNull(currentIndex) ?: return
        if (!force && lastPresentedWordId == card.wordId) {
            return
        }
        currentCardPresentedAt = nowProvider()
        lastPresentedWordId = card.wordId
        eventRecorder.record(
            StudyEvent(
                wordId = card.wordId,
                eventType = StudyEventType.CARD_PRESENTED,
                happenedAt = currentCardPresentedAt,
                metadata = studyEventMetadataOf(
                    "mode" to "card",
                    "hasExample" to (card.exampleSentence != null),
                    StudyEventMetadataKey.QUEUE_BUCKET to card.queueBucket,
                    StudyEventMetadataKey.GOAL_SCOPE to "daily",
                ),
            ),
        )
    }

    private fun cardFeedbackMetadata(
        card: StudyCardItem,
        responseLatencyMs: Long,
        skipped: Boolean,
        requeued: Boolean,
    ): String? =
        studyEventMetadataOf(
            "mode" to "card",
            "progress" to "${currentIndex + 1}/${queue.size}",
            "requeued" to requeued,
            StudyEventMetadataKey.QUEUE_BUCKET to card.queueBucket,
            StudyEventMetadataKey.RESPONSE_LATENCY_MS to responseLatencyMs,
            StudyEventMetadataKey.SKIPPED to skipped,
            StudyEventMetadataKey.GOAL_SCOPE to "daily",
        )

    private fun clearDeferredSessionState() {
        deferredSessionReason = null
        deferredCompletedCount = null
    }
}

suspend fun loadStudyViewModel(context: Context): StudyViewModel {
    syncBuiltInCatalogToDatabase(context)
    val appContext = context.applicationContext
    val settingsRepository = buildSettingsRepository(appContext)
    val settings = settingsRepository.getSettings()
    val studyRepository = RoomStudyRepository(buildDanciDatabase(appContext).studyDao())
    val bookRepository = buildBookRepository(appContext)
    val activeBook = settings.activeBookId?.let { bookRepository.getBook(it) } ?: bookRepository.getAllBooks().first()
    val initialRecords = studyRepository.getAllLearningRecords().associateBy(LearningRecord::wordId)
    val now = Instant.now()
    val queueBuckets = ReviewPriorityEngine(nowProvider = { now })
        .rank(initialRecords.values.toList(), now)
        .associate { ranked -> ranked.wordId to ranked.bucket }
    val queue = StudyQueueBuilder().buildFromWords(
        words = bookRepository.getWords(activeBook.id),
        queueBuckets = queueBuckets,
    )
    val aiMemoryRepository = buildAiMemoryRepository(appContext)
    val pronunciationSourceRepository = buildPronunciationSourceRepository(appContext)
    return StudyViewModel(
        initialQueue = queue,
        initialRecords = queue.associate { card ->
            card.wordId to (initialRecords[card.wordId] ?: defaultLearningRecord(card.wordId))
        },
        eventRecorder = aiMemoryRepository,
        learningRecordRecorder = aiMemoryRepository,
        settingsRepository = settingsRepository,
        pronunciationSourceRegistry = PronunciationSourceRegistry(
            sourceRepository = pronunciationSourceRepository,
            voicePackRepository = buildVoicePackRepository(appContext),
            settingsRepository = settingsRepository,
            aiProfileRepository = buildAiProfileRepository(appContext),
        ),
    )
}

private fun CardFeedback.toEventFeedback(): String =
    when (this) {
        CardFeedback.NOT_KNOWN -> "wrong"
        CardFeedback.FUZZY -> "fuzzy"
        CardFeedback.KNOWN -> "correct"
    }
