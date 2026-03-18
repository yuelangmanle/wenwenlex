package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizGeneratorTest {
    @Test
    fun prefersConfusionPairsOverGenericDistractors() {
        val question = QuizGenerator().createQuestion(
            target = preciseWord(),
            confusionWords = listOf(accurateWord(), exactWord()),
            fallbackWords = listOf(randomDistractorWord()),
        )

        assertEquals("精确的", question.correctAnswer)
        assertTrue(question.options.contains("精确的"))
        assertTrue(question.options.contains("准确的"))
    }

    private fun preciseWord(): Word =
        Word(
            id = 1L,
            lemma = "precise",
            meanings = listOf("精确的"),
        )

    private fun accurateWord(): Word =
        Word(
            id = 2L,
            lemma = "accurate",
            meanings = listOf("准确的"),
        )

    private fun exactWord(): Word =
        Word(
            id = 3L,
            lemma = "exact",
            meanings = listOf("确切的"),
        )

    private fun randomDistractorWord(): Word =
        Word(
            id = 4L,
            lemma = "casual",
            meanings = listOf("随意的"),
        )
}
