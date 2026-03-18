package com.yueliangmanle.danci.feature.quiz

import android.content.Context
import com.yueliangmanle.danci.core.data.findRelatedWords
import com.yueliangmanle.danci.core.data.loadBuiltInWord
import com.yueliangmanle.danci.core.data.loadBuiltInWords
import com.yueliangmanle.danci.core.study.QuizGenerator
import com.yueliangmanle.danci.core.study.QuizQuestion
import com.yueliangmanle.danci.core.study.primaryMeaning

data class QuizUiState(
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val correctAnswer: String = "",
    val explanation: String? = null,
)

class QuizViewModel(
    private val question: QuizQuestion,
) {
    fun buildUiState(selectedOption: String? = null): QuizUiState =
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

    return QuizViewModel(question = question)
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
