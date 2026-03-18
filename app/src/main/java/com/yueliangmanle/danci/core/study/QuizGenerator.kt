package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.Word

data class QuizQuestion(
    val targetWordId: Long,
    val prompt: String,
    val correctAnswer: String,
    val options: List<String>,
    val optionWordIds: Map<String, Long> = emptyMap(),
)

class QuizGenerator {
    fun createQuestion(
        target: Word,
        confusionWords: List<Word>,
        fallbackWords: List<Word>,
    ): QuizQuestion {
        val correctAnswer = target.primaryMeaning()
        val distractorWords = (confusionWords + fallbackWords)
            .filterNot { it.id == target.id }
            .filter { candidate ->
                val meaning = candidate.primaryMeaning()
                meaning.isNotBlank() && meaning != correctAnswer
            }
            .distinctBy { it.primaryMeaning() }
            .take(3)
        val options = (listOf(correctAnswer) + distractorWords.map { it.primaryMeaning() }).distinct()
        val optionWordIds = (distractorWords + target).associate { word ->
            word.primaryMeaning() to word.id
        }

        return QuizQuestion(
            targetWordId = target.id,
            prompt = target.lemma,
            correctAnswer = correctAnswer,
            options = options,
            optionWordIds = optionWordIds,
        )
    }
}

fun Word.primaryMeaning(): String = meanings.firstOrNull().orEmpty()
