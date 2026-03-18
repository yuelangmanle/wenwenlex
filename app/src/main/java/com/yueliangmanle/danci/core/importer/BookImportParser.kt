package com.yueliangmanle.danci.core.importer

import java.io.InputStream

interface BookImportParser {
    fun parse(inputStream: InputStream): ImportedBook
}

data class ImportedBook(
    val metadata: ImportedBookMetadata,
    val words: List<ImportedWord>,
)

data class ImportedBookMetadata(
    val id: String,
    val title: String,
    val description: String? = null,
    val language: String = "en",
    val category: String = "custom",
    val sourceType: String = "imported",
)

data class ImportedWord(
    val text: String,
    val phonetic: String? = null,
    val phoneticUk: String? = null,
    val phoneticUs: String? = null,
    val meanings: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val similarWords: List<String> = emptyList(),
    val confusingWords: List<String> = emptyList(),
    val wordForms: List<String> = emptyList(),
    val root: String? = null,
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
)

fun String.splitPipedValues(): List<String> =
    split("|").map(String::trim).filter(String::isNotEmpty)
