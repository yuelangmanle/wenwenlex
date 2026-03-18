package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.importer.ImportedWord

data class StudyCardItem(
    val wordId: Long,
    val word: String,
    val phonetic: String? = null,
    val meanings: List<String> = emptyList(),
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
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
}
