package com.yueliangmanle.danci.feature.study

import android.content.Context
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.defaultLearningRecord
import com.yueliangmanle.danci.core.importer.JsonBookImporter
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

fun loadStudyViewModel(context: Context): StudyViewModel {
    val book = context.assets.open("books/cet4.json").use(JsonBookImporter()::parse)
    val queue = StudyQueueBuilder().buildFromImportedWords(book.words)
    return StudyViewModel(
        initialQueue = queue,
        eventRecorder = buildAiMemoryRepository(context),
    )
}
