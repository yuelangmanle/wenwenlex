package com.yueliangmanle.danci.core.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvBookImporterTest {
    @Test
    fun parsesRelationsAndWordFormsFromCsv() {
        val csv = """
            word,phonetic,meaning,synonyms,antonyms,similar_words,word_forms
            abandon,/əˈbændən/,"放弃","give up|quit","continue","abundant|absorb","abandoned|abandoning|abandonment"
        """.trimIndent()

        val result = CsvBookImporter().parse(csv.byteInputStream())
        val word = result.words.single()

        assertEquals(listOf("give up", "quit"), word.synonyms)
        assertEquals(listOf("continue"), word.antonyms)
        assertEquals(listOf("abundant", "absorb"), word.similarWords)
        assertEquals(listOf("abandoned", "abandoning", "abandonment"), word.wordForms)
    }
}
