package com.yueliangmanle.danci.feature.study

import android.content.Context
import com.yueliangmanle.danci.core.data.defaultLearningRecord
import com.yueliangmanle.danci.core.importer.JsonBookImporter
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.FeedbackMapper
import com.yueliangmanle.danci.core.study.StudyCardItem
import com.yueliangmanle.danci.core.study.StudyQueueBuilder
import java.time.Instant

data class StudyUiState(
    val sessionTitle: String = "卡片学习",
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
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private val queue = initialQueue.toMutableList()
    private val records = initialQueue.associate { card ->
        card.wordId to defaultLearningRecord(card.wordId)
    }.toMutableMap()
    private var currentIndex = 0

    fun buildUiState(): StudyUiState {
        val card = queue.getOrNull(currentIndex)
        if (card == null) {
            return StudyUiState(
                currentWord = "今日学习完成",
                meanings = listOf("可以回到首页继续安排下一轮复习。"),
                progressText = "${queue.size} / ${queue.size}",
                isSessionComplete = true,
            )
        }

        return StudyUiState(
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
        records[card.wordId] = feedbackMapper.applyCardFeedback(
            current = currentRecord,
            feedback = feedback,
            answeredAt = nowProvider(),
        )

        if (feedback == CardFeedback.NOT_KNOWN) {
            queue.add(card)
        }

        currentIndex += 1
        return buildUiState()
    }

    fun currentRecord(wordId: Long): LearningRecord = records.getValue(wordId)
}

fun loadStudyViewModel(context: Context): StudyViewModel {
    val book = context.assets.open("books/cet4.json").use(JsonBookImporter()::parse)
    val queue = StudyQueueBuilder().buildFromImportedWords(book.words)
    return StudyViewModel(initialQueue = queue)
}
