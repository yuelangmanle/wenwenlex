package com.yueliangmanle.danci.feature.study

import android.content.Context
import com.yueliangmanle.danci.core.ai.AiPlanAdjustmentResult
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.defaultLearningRecord
import com.yueliangmanle.danci.core.data.syncBuiltInCatalogToDatabase
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.FeedbackMapper
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
)

data class SessionCheckpointRequest(
    val completedCount: Int,
    val mistakeBurst: Int,
    val reason: String,
)

class StudyViewModel(
    initialQueue: List<StudyCardItem>,
    private val feedbackMapper: FeedbackMapper = FeedbackMapper(),
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
    private var pendingCheckpointRequest: SessionCheckpointRequest? = null
    private var checkpointTitle: String? = null
    private var checkpointSuggestion: String? = null
    private var checkpointSourceLabel: String? = null

    init {
        recordCurrentCardPresentedIfNeeded()
    }

    fun buildUiState(): StudyUiState {
        val card = queue.getOrNull(currentIndex)
        if (card == null) {
            return StudyUiState(
                currentWordId = queue.lastOrNull()?.wordId ?: 0L,
                currentWord = "今日学习完成",
                meanings = listOf("可以回到首页继续安排下一轮复习。"),
                progressText = "${queue.size} / ${queue.size}",
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
            checkpointTitle = checkpointTitle,
            checkpointSuggestion = checkpointSuggestion,
            checkpointSourceLabel = checkpointSourceLabel,
        )
    }

    fun submitFeedback(feedback: CardFeedback): StudyUiState {
        val card = queue.getOrNull(currentIndex) ?: return buildUiState()
        val currentRecord = records.getValue(card.wordId)
        val answeredAt = nowProvider()
        records[card.wordId] = feedbackMapper.applyCardFeedback(
            current = currentRecord,
            feedback = feedback,
            answeredAt = answeredAt,
        )
        eventRecorder.record(
            StudyEvent(
                wordId = card.wordId,
                eventType = StudyEventType.CARD_FEEDBACK,
                feedback = feedback.name.lowercase(),
                isCorrect = feedback == CardFeedback.KNOWN,
                happenedAt = answeredAt,
                elapsedMillis = Duration.between(currentCardPresentedAt, answeredAt).toMillis().coerceAtLeast(0L),
                metadata = studyEventMetadataOf(
                    "mode" to "card",
                    "progress" to "${currentIndex + 1}/${queue.size}",
                    "requeued" to (feedback == CardFeedback.NOT_KNOWN),
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

suspend fun loadStudyViewModel(context: Context): StudyViewModel {
    syncBuiltInCatalogToDatabase(context)
    val settings = buildSettingsRepository(context).getSettings()
    val bookRepository = buildBookRepository(context)
    val activeBook = settings.activeBookId?.let { bookRepository.getBook(it) } ?: bookRepository.getAllBooks().first()
    val queue = StudyQueueBuilder().buildFromWords(bookRepository.getWords(activeBook.id))
    return StudyViewModel(
        initialQueue = queue,
        eventRecorder = buildAiMemoryRepository(context),
    )
}
