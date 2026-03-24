package com.yueliangmanle.danci.feature.study

import android.content.Context
import com.yueliangmanle.danci.core.ai.AiPlanAdjustmentResult
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.defaultLearningRecord
import com.yueliangmanle.danci.core.data.syncBuiltInCatalogToDatabase
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.StudySession
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.FeedbackMapper
import com.yueliangmanle.danci.core.study.StudyCardItem
import com.yueliangmanle.danci.core.study.StudyLaunchMode
import com.yueliangmanle.danci.core.study.StudyQueueEmptyState
import com.yueliangmanle.danci.core.study.StudyQueuePlanner
import com.yueliangmanle.danci.core.study.WordPassPolicy
import com.yueliangmanle.danci.core.study.WordPassStep
import com.yueliangmanle.danci.core.study.defaultStudyGroupSize
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
    val passStep: WordPassStep = WordPassStep.MEANING,
    val passStepLabel: String? = null,
    val stepPrompt: String? = null,
    val isLoadingQueue: Boolean = true,
    val emptyState: StudyQueueEmptyState? = null,
    val groupSummaryTitle: String? = null,
    val groupSummaryBody: String? = null,
    val showContinueNextGroup: Boolean = false,
    val isSessionComplete: Boolean = false,
    val checkpointTitle: String? = null,
    val checkpointSuggestion: String? = null,
    val checkpointSourceLabel: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class SessionCheckpointRequest(
    val completedCount: Int,
    val mistakeBurst: Int,
    val reason: String,
)

data class LoadedStudySession(
    val viewModel: StudyViewModel,
    val resolvedMode: StudyLaunchMode,
)

class StudyViewModel(
    initialQueue: List<StudyCardItem>,
    private val emptyState: StudyQueueEmptyState? = null,
    private val sessionId: Long? = null,
    private val feedbackMapper: FeedbackMapper = FeedbackMapper(),
    private val wordPassPolicy: WordPassPolicy = WordPassPolicy(spellingEnabled = true),
    private val eventRecorder: StudyEventRecorder = NoOpStudyEventRecorder,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private val queue = initialQueue.toMutableList()
    private val records = initialQueue.associate { card ->
        card.wordId to defaultLearningRecord(card.wordId)
    }.toMutableMap()
    private var currentIndex = 0
    private var currentCardPresentedAt: Instant = nowProvider()
    private var lastPresentedWordId: Long? = null
    private var completedCount = 0
    private var recentMistakeBurst = 0
    private val baseGroupSize = initialQueue.size
    private var currentGroupSize = initialQueue.size
    private var completedInCurrentGroup = 0
    private var passedInCurrentGroup = 0
    private var retryInCurrentGroup = 0
    private var currentPassStep = WordPassStep.MEANING
    private var currentWordWorstFeedback: CardFeedback? = null
    private var groupSummaryTitle: String? = null
    private var groupSummaryBody: String? = null
    private var pendingCheckpointRequest: SessionCheckpointRequest? = null
    private var checkpointTitle: String? = null
    private var checkpointSuggestion: String? = null
    private var checkpointSourceLabel: String? = null

    init {
        recordCurrentCardPresentedIfNeeded()
    }

    fun buildUiState(): StudyUiState {
        if (queue.isEmpty()) {
            return StudyUiState(
                isLoadingQueue = false,
                emptyState = emptyState,
                checkpointTitle = checkpointTitle,
                checkpointSuggestion = checkpointSuggestion,
                checkpointSourceLabel = checkpointSourceLabel,
            )
        }
        if (groupSummaryTitle != null) {
            return StudyUiState(
                progressText = "${currentIndex.coerceAtMost(queue.size)} / ${queue.size}",
                passStep = currentPassStep,
                isLoadingQueue = false,
                groupSummaryTitle = groupSummaryTitle,
                groupSummaryBody = groupSummaryBody,
                showContinueNextGroup = true,
                checkpointTitle = checkpointTitle,
                checkpointSuggestion = checkpointSuggestion,
                checkpointSourceLabel = checkpointSourceLabel,
            )
        }
        val card = queue.getOrNull(currentIndex)
        if (card == null) {
            return StudyUiState(
                currentWordId = queue.lastOrNull()?.wordId ?: 0L,
                currentWord = "今日学习完成",
                meanings = listOf("可以回到首页继续安排下一轮复习。"),
                progressText = "${queue.size} / ${queue.size}",
                isLoadingQueue = false,
                isSessionComplete = true,
                checkpointTitle = checkpointTitle,
                checkpointSuggestion = checkpointSuggestion,
                checkpointSourceLabel = checkpointSourceLabel,
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
            passStep = currentPassStep,
            passStepLabel = currentPassStep.label(),
            stepPrompt = currentPassStep.prompt(),
            isLoadingQueue = false,
            checkpointTitle = checkpointTitle,
            checkpointSuggestion = checkpointSuggestion,
            checkpointSourceLabel = checkpointSourceLabel,
        )
    }

    fun submitFeedback(feedback: CardFeedback): StudyUiState {
        if (groupSummaryTitle != null) {
            return buildUiState()
        }
        val card = queue.getOrNull(currentIndex) ?: return buildUiState()
        val answeredAt = nowProvider()
        eventRecorder.record(
            StudyEvent(
                sessionId = sessionId,
                wordId = card.wordId,
                eventType = StudyEventType.CARD_FEEDBACK,
                feedback = feedback.name.lowercase(),
                isCorrect = feedback == CardFeedback.KNOWN,
                happenedAt = answeredAt,
                elapsedMillis = Duration.between(currentCardPresentedAt, answeredAt).toMillis().coerceAtLeast(0L),
                metadata = studyEventMetadataOf(
                    "mode" to "card",
                    "step" to currentPassStep.name.lowercase(),
                    "progress" to "${currentIndex + 1}/${queue.size}",
                    "requeued" to false,
                ),
            ),
        )
        if (feedback != CardFeedback.KNOWN) {
            currentWordWorstFeedback = currentWordWorstFeedback.worseThan(feedback)
            currentPassStep = wordPassPolicy.advance(currentPassStep, passed = false).nextStep
            return buildUiState()
        }

        val nextStep = wordPassPolicy.advance(currentPassStep, passed = true).nextStep
        if (nextStep != WordPassStep.DONE) {
            currentPassStep = nextStep
            return buildUiState()
        }

        val effectiveFeedback = currentWordWorstFeedback ?: CardFeedback.KNOWN
        val currentRecord = records.getValue(card.wordId)
        records[card.wordId] = feedbackMapper.applyCardFeedback(
            current = currentRecord,
            feedback = effectiveFeedback,
            answeredAt = answeredAt,
        )
        if (effectiveFeedback != CardFeedback.KNOWN) {
            queue.add(card)
            retryInCurrentGroup += 1
        } else {
            passedInCurrentGroup += 1
        }

        completedCount += 1
        completedInCurrentGroup += 1
        recentMistakeBurst = if (effectiveFeedback == CardFeedback.KNOWN) 0 else recentMistakeBurst + 1
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
        currentPassStep = WordPassStep.MEANING
        currentWordWorstFeedback = null
        if (completedInCurrentGroup == currentGroupSize && currentGroupSize > 0) {
            groupSummaryTitle = "本组 $currentGroupSize 词已完成"
            groupSummaryBody = buildString {
                append("已通过 $passedInCurrentGroup 词")
                append("，待回拉 $retryInCurrentGroup 词")
                append("，已进入拼写：是")
            }
        } else if (queue.getOrNull(currentIndex) != null) {
            recordCurrentCardPresentedIfNeeded(force = true)
        }
        return buildUiState()
    }

    fun continueNextGroup(): StudyUiState {
        if (groupSummaryTitle == null) {
            return buildUiState()
        }
        groupSummaryTitle = null
        groupSummaryBody = null
        completedInCurrentGroup = 0
        passedInCurrentGroup = 0
        retryInCurrentGroup = 0
        currentGroupSize = minOf(baseGroupSize, (queue.size - currentIndex).coerceAtLeast(0))
        if (queue.getOrNull(currentIndex) != null) {
            currentPassStep = WordPassStep.MEANING
            currentWordWorstFeedback = null
            recordCurrentCardPresentedIfNeeded(force = true)
        }
        return buildUiState()
    }

    fun openCurrentWordDetail() {
        val card = queue.getOrNull(currentIndex) ?: return
        eventRecorder.record(
            StudyEvent(
                sessionId = sessionId,
                wordId = card.wordId,
                eventType = StudyEventType.DETAIL_OPENED,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "card",
                    "source" to "study",
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

    fun applyCheckpointSuggestion(result: AiPlanAdjustmentResult): StudyUiState {
        checkpointTitle = if (result.source == PlanSource.AI) {
            "AI 阶段建议"
        } else {
            "本地阶段建议"
        }
        checkpointSuggestion = listOfNotNull(result.summary, result.checkpointAdvice).joinToString("\n")
        checkpointSourceLabel = if (result.source == PlanSource.AI) "AI 生成" else "本地兜底"
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
                sessionId = sessionId,
                wordId = card.wordId,
                eventType = StudyEventType.CARD_PRESENTED,
                happenedAt = currentCardPresentedAt,
                metadata = studyEventMetadataOf(
                    "mode" to "card",
                    "hasExample" to (card.exampleSentence != null),
                ),
            ),
        )
    }
}

private fun CardFeedback?.worseThan(candidate: CardFeedback): CardFeedback =
    when {
        this == CardFeedback.NOT_KNOWN || candidate == CardFeedback.NOT_KNOWN -> CardFeedback.NOT_KNOWN
        this == CardFeedback.FUZZY || candidate == CardFeedback.FUZZY -> CardFeedback.FUZZY
        else -> CardFeedback.KNOWN
    }

private fun WordPassStep.label(): String =
    when (this) {
        WordPassStep.MEANING -> "步骤 1/3 · 识义"
        WordPassStep.RECALL -> "步骤 2/3 · 回想"
        WordPassStep.SPELLING -> "步骤 3/3 · 拼写"
        WordPassStep.DONE -> "本词已完成"
    }

private fun WordPassStep.prompt(): String =
    when (this) {
        WordPassStep.MEANING -> "先确认词义和例句，再进入下一步。"
        WordPassStep.RECALL -> "根据义项或例句回想英文单词。"
        WordPassStep.SPELLING -> "进入拼写确认，通过后本词才算过关。"
        WordPassStep.DONE -> "当前单词已完成。"
    }

suspend fun loadStudyViewModel(
    context: Context,
    launchMode: StudyLaunchMode? = null,
): LoadedStudySession {
    syncBuiltInCatalogToDatabase(context)
    val settings = buildSettingsRepository(context).getSettings()
    val bookRepository = buildBookRepository(context)
    val studyRepository = RoomStudyRepository(buildDanciDatabase(context).studyDao())
    val activeBook = settings.activeBookId?.let { bookRepository.getBook(it) } ?: bookRepository.getAllBooks().first()
    val words = bookRepository.getWords(activeBook.id)
    val recordsByWordId = studyRepository.getAllLearningRecords().associateBy { it.wordId }
    val planner = StudyQueuePlanner()
    val mode = planner.resolveMode(launchMode, words, recordsByWordId)
    val plan = planner.plan(
        mode = mode,
        words = words,
        recordsByWordId = recordsByWordId,
        groupSize = defaultStudyGroupSize(mode),
    )
    val sessionId = studyRepository.startSession(
        StudySession(
            mode = mode.storageValue,
            targetBookId = activeBook.id,
            scopeType = "active_book",
            scopeRef = activeBook.id,
            groupSize = defaultStudyGroupSize(mode),
            currentGroupIndex = 0,
            startedAt = Instant.now(),
            plannedCount = plan.queue.size,
        ),
    )
    return LoadedStudySession(
        viewModel = StudyViewModel(
            initialQueue = plan.queue,
            emptyState = plan.emptyState,
            sessionId = sessionId,
            eventRecorder = buildAiMemoryRepository(context),
        ),
        resolvedMode = mode,
    )
}
