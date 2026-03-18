package com.yueliangmanle.danci.core.model

import java.time.Instant

data class Word(
    val id: Long = 0,
    val lemma: String,
    val phonetic: String? = null,
    val phoneticUk: String? = null,
    val phoneticUs: String? = null,
    val phoneticSource: String = PHONETIC_SOURCE_EMPTY,
    val phoneticStatus: String = PHONETIC_STATUS_EMPTY,
    val phoneticUpdatedAt: Instant? = null,
    val partOfSpeech: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val similarWords: List<String> = emptyList(),
    val confusingWords: List<String> = emptyList(),
    val wordForms: List<String> = emptyList(),
    val root: String? = null,
    val tags: List<String> = emptyList(),
    val frequencyRank: Int? = null,
    val pronunciationUrl: String? = null,
)

const val PHONETIC_SOURCE_EMPTY = "empty"
const val PHONETIC_SOURCE_BUILTIN = "builtin"
const val PHONETIC_SOURCE_IMPORTED = "imported"
const val PHONETIC_SOURCE_AI = "ai_generated"
const val PHONETIC_SOURCE_MANUAL = "manual"
const val PHONETIC_SOURCE_LEGACY = "legacy"

const val PHONETIC_STATUS_EMPTY = "empty"
const val PHONETIC_STATUS_PARTIAL = "partial"
const val PHONETIC_STATUS_COMPLETE = "complete"
