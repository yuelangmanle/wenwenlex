package com.yueliangmanle.danci.feature.quiz

import android.content.Context
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.findRelatedWords
import com.yueliangmanle.danci.core.data.loadBuiltInWord
import com.yueliangmanle.danci.core.data.loadBuiltInWords
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import com.yueliangmanle.danci.core.study.QuizGenerator
import com.yueliangmanle.danci.core.study.QuizQuestion
import com.yueliangmanle.danci.core.study.primaryMeaning
import java.time.Duration
import java.time.Instant

data class QuizUiState(
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val correctAnswer: String = "",
    val explanation: String? = null,
)

class QuizViewModel(
    private val question: QuizQuestion,
    private val eventRecorder: StudyEventRecorder = NoOpStudyEventRecorder,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private val presentedAt: Instant = nowProvider()
    private var selectedOption: String? = null

    init {
        eventRecorder.record(
            StudyEvent(
                wordId = question.targetWordId,
                eventType = StudyEventType.QUIZ_STARTED,
                happenedAt = presentedAt,
                metadata = studyEventMetadataOf(
                    "mode" to "quiz",
                    "optionCount" to question.options.size,
                ),
            ),
        )
    }

    fun buildUiState(): QuizUiState =
        QuizUiState(
            prompt = question.prompt,
            options = question.options,
            selectedOption = selectedOption,
            correctAnswer = question.correctAnswer,
            explanation = selectedOption?.let { option ->
                if (option == question.correctAnswer) {
                    "答对了，${question.prompt} 对应 ${question.correctAnswer}。"
                } else {
                    "正确答案是 ${question.correctAnswer}。"
                }
            },
        )

    fun selectOption(option: String): QuizUiState {
        if (selectedOption != null) {
            return buildUiState()
        }
        selectedOption = option
        val answeredAt = nowProvider()
        val isCorrect = option == question.correctAnswer
        eventRecorder.record(
            StudyEvent(
                wordId = question.targetWordId,
                eventType = StudyEventType.QUIZ_ANSWERED,
                feedback = if (isCorrect) "correct" else "wrong",
                isCorrect = isCorrect,
                happenedAt = answeredAt,
                elapsedMillis = Duration.between(presentedAt, answeredAt).toMillis().coerceAtLeast(0L),
                metadata = studyEventMetadataOf(
                    "mode" to "quiz",
                    "selectedOption" to option,
                    "confusedWordId" to question.optionWordIds[option]?.takeIf { !isCorrect },
                    "relationType" to if (isCorrect) null else "confused_with",
                ),
            ),
        )
        return buildUiState()
    }

    fun openWordDetail() {
        eventRecorder.record(
            StudyEvent(
                wordId = question.targetWordId,
                eventType = StudyEventType.DETAIL_OPENED,
                happenedAt = nowProvider(),
                metadata = studyEventMetadataOf(
                    "mode" to "quiz",
                    "source" to "quiz_review",
                ),
            ),
        )
    }
}

fun loadQuizViewModel(
    context: Context,
    wordId: Long,
): QuizViewModel {
    val words = loadBuiltInWords(context)
    val target = loadBuiltInWord(context, wordId) ?: words.first()
    val confusionWords = findRelatedWords(target, words)
    val fallbackWords = words.filterNot { it.id == target.id }
    val question = QuizGenerator().createQuestion(
        target = target,
        confusionWords = confusionWords,
        fallbackWords = fallbackWords,
    ).ensureMinimumOptions(target.primaryMeaning(), fallbackWords.map { it.primaryMeaning() })

    return QuizViewModel(
        question = question,
        eventRecorder = buildAiMemoryRepository(context),
    )
}

private fun QuizQuestion.ensureMinimumOptions(
    correctAnswer: String,
    fallbackMeanings: List<String>,
): QuizQuestion {
    if (options.size >= 4) {
        return this
    }
    val filledOptions = (options + fallbackMeanings)
        .filter(String::isNotBlank)
        .distinct()
        .take(4)
        .ifEmpty { listOf(correctAnswer) }

    return copy(options = filledOptions)
}
