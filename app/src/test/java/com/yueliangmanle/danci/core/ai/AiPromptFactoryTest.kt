package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.Word
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptFactoryTest {
    @Test
    fun buildsRelationAwarePromptForWordHelp() {
        val prompt = AiPromptFactory().wordHelpPrompt(
            word = abandonWord(),
            request = AiWordHelpRequest.RELATION_DIFFERENCE,
        )

        assertTrue(prompt.contains("近义词"))
        assertTrue(prompt.contains("反义词"))
        assertTrue(prompt.contains("拼写相近词"))
    }

    private fun abandonWord(): Word =
        Word(
            id = 1L,
            lemma = "abandon",
            meanings = listOf("放弃"),
            synonyms = listOf("give up"),
            antonyms = listOf("persist"),
            similarWords = listOf("abundant"),
            wordForms = listOf("abandoned", "abandoning"),
            root = "bandon",
        )
}
