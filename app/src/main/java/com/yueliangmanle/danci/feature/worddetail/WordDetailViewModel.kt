package com.yueliangmanle.danci.feature.worddetail

import android.content.Context
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.loadBuiltInWord
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import java.time.Instant

data class WordDetailUiState(
    val wordId: Long = 0L,
    val word: String = "",
    val phonetic: String? = null,
    val meanings: List<String> = emptyList(),
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val similarWords: List<String> = emptyList(),
    val confusingWords: List<String> = emptyList(),
    val wordForms: List<String> = emptyList(),
    val root: String? = null,
)

class WordDetailViewModel(
    private val word: Word,
    private val eventRecorder: StudyEventRecorder = NoOpStudyEventRecorder,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    init {
        eventRecorder.record(
            StudyEvent(
                wordId = word.id,
                eventType = StudyEventType.DETAIL_OPENED,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "detail",
                    "sections" to detailSections().joinToString(","),
                ),
            ),
        )
    }

    fun buildUiState(): WordDetailUiState =
        WordDetailUiState(
            wordId = word.id,
            word = word.lemma,
            phonetic = word.phonetic,
            meanings = word.meanings,
            exampleSentence = word.exampleSentence,
            exampleTranslation = word.exampleTranslation,
            synonyms = word.synonyms,
            antonyms = word.antonyms,
            similarWords = word.similarWords,
            confusingWords = word.confusingWords,
            wordForms = word.wordForms,
            root = word.root,
        )

    fun onAiMemoryClick() {
        recordAiAction("memory_helper")
    }

    fun onAiContrastClick() {
        recordAiAction("contrast")
    }

    fun onStartQuizClick() {
        eventRecorder.record(
            StudyEvent(
                wordId = word.id,
                eventType = StudyEventType.QUIZ_STARTED,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "detail",
                    "source" to "word_detail",
                ),
            ),
        )
    }

    private fun recordAiAction(action: String) {
        eventRecorder.record(
            StudyEvent(
                wordId = word.id,
                eventType = StudyEventType.AI_ACTION,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "detail",
                    "action" to action,
                ),
            ),
        )
    }

    private fun detailSections(): List<String> =
        buildList {
            if (word.synonyms.isNotEmpty()) add("synonyms")
            if (word.antonyms.isNotEmpty()) add("antonyms")
            if (word.similarWords.isNotEmpty()) add("similar_words")
            if (word.confusingWords.isNotEmpty()) add("confusing_words")
            if (word.wordForms.isNotEmpty()) add("word_forms")
            if (!word.root.isNullOrBlank()) add("root")
        }
}

fun loadWordDetailViewModel(
    context: Context,
    wordId: Long,
): WordDetailViewModel {
    val word = requireNotNull(loadBuiltInWord(context, wordId)) {
        "Expected built-in word for id=$wordId"
    }
    return WordDetailViewModel(
        word = word,
        eventRecorder = buildAiMemoryRepository(context),
    )
}
