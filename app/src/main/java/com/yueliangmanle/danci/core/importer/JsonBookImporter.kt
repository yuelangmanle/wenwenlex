package com.yueliangmanle.danci.core.importer

import java.io.InputStream
import org.json.JSONArray
import org.json.JSONObject

class JsonBookImporter : BookImportParser {
    override fun parse(inputStream: InputStream): ImportedBook {
        val root = JSONObject(inputStream.bufferedReader().use { it.readText() })
        val words = root.getJSONArray("words").toImportedWords()

        return ImportedBook(
            metadata = ImportedBookMetadata(
                id = root.getString("id"),
                title = root.getString("title"),
                description = root.optString("description").ifBlank { null },
                language = root.optString("language").ifBlank { "en" },
                category = root.optString("category").ifBlank { "custom" },
                sourceType = root.optString("sourceType").ifBlank { "imported" },
            ),
            words = words,
        )
    }
}

private fun JSONArray.toImportedWords(): List<ImportedWord> =
    List(length()) { index ->
        val item = getJSONObject(index)
        ImportedWord(
            text = item.getString("word"),
            phonetic = item.optString("phonetic").ifBlank { null },
            phoneticUk = item.optString("phonetic_uk").ifBlank { null },
            phoneticUs = item.optString("phonetic_us").ifBlank { null },
            meanings = item.optJSONArray("meaning").toStringList(),
            synonyms = item.optJSONArray("synonyms").toStringList(),
            antonyms = item.optJSONArray("antonyms").toStringList(),
            similarWords = item.optJSONArray("similar_words").toStringList(),
            confusingWords = item.optJSONArray("confusing_words").toStringList(),
            wordForms = item.optJSONArray("word_forms").toStringList(),
            root = item.optString("root").ifBlank { null },
            exampleSentence = item.optString("example_sentence").ifBlank { null },
            exampleTranslation = item.optString("example_translation").ifBlank { null },
        )
    }

private fun JSONArray?.toStringList(): List<String> =
    if (this == null) {
        emptyList()
    } else {
        List(length()) { index -> getString(index) }
    }
