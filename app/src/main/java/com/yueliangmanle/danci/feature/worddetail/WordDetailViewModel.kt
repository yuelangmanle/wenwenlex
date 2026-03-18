package com.yueliangmanle.danci.feature.worddetail

import android.content.Context
import com.yueliangmanle.danci.core.data.loadBuiltInWord
import com.yueliangmanle.danci.core.model.Word

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
) {
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
}

fun loadWordDetailViewModel(
    context: Context,
    wordId: Long,
): WordDetailViewModel {
    val word = requireNotNull(loadBuiltInWord(context, wordId)) {
        "Expected built-in word for id=$wordId"
    }
    return WordDetailViewModel(word = word)
}
