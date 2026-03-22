package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.importer.ImportedWord
import com.yueliangmanle.danci.core.model.Word

data class StudyCardItem(
    val wordId: Long,
    val word: String,
    val phonetic: String? = null,
    val meanings: List<String> = emptyList(),
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
    val queueBucket: String = "new",
)

class StudyQueueBuilder {
    fun buildFromImportedWords(words: List<ImportedWord>): List<StudyCardItem> =
        words.mapIndexed { index, word ->
            StudyCardItem(
                wordId = index.toLong() + 1L,
                word = word.text,
                phonetic = word.phonetic,
                meanings = word.meanings,
                exampleSentence = word.exampleSentence,
                exampleTranslation = word.exampleTranslation,
            )
        }

    fun buildFromWords(words: List<Word>): List<StudyCardItem> =
        words.map { word ->
            StudyCardItem(
                wordId = word.id,
                word = word.lemma,
                phonetic = word.phoneticUk ?: word.phoneticUs ?: word.phonetic,
                meanings = word.meanings,
                exampleSentence = word.exampleSentence,
                exampleTranslation = word.exampleTranslation,
            )
        }
}
