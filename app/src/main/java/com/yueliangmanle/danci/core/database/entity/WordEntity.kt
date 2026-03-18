package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["lemma"], unique = true)],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lemma: String,
    val phonetic: String? = null,
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
