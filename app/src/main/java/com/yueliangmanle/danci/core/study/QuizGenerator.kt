package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.Word

data class QuizQuestion(
    val prompt: String,
    val correctAnswer: String,
    val options: List<String>,
)

class QuizGenerator {
    fun createQuestion(
        target: Word,
        confusionWords: List<Word>,
        fallbackWords: List<Word>,
    ): QuizQuestion {
        val correctAnswer = target.primaryMeaning()
        val distractors = (confusionWords + fallbackWords)
            .filterNot { it.id == target.id }
            .map(Word::primaryMeaning)
            .filter(String::isNotBlank)
            .filterNot { it == correctAnswer }
            .distinct()
            .take(3)

        return QuizQuestion(
            prompt = target.lemma,
            correctAnswer = correctAnswer,
            options = (listOf(correctAnswer) + distractors).distinct(),
        )
    }
}

fun Word.primaryMeaning(): String = meanings.firstOrNull().orEmpty()
