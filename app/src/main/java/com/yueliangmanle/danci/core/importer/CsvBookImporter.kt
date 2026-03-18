package com.yueliangmanle.danci.core.importer

import java.io.InputStream

class CsvBookImporter : BookImportParser {
    override fun parse(inputStream: InputStream): ImportedBook {
        val rows = inputStream.bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).toList()
        }
        require(rows.isNotEmpty()) { "CSV content is empty" }

        val headers = parseCsvRow(rows.first())
        val words = rows.drop(1).map { row ->
            val values = parseCsvRow(row)
            val data = headers.zip(values).toMap()
            ImportedWord(
                text = data.getValue("word").trim(),
                phonetic = data["phonetic"]?.ifBlank { null },
                meanings = data["meaning"].orEmpty().splitPipedValues(),
                synonyms = data["synonyms"].orEmpty().splitPipedValues(),
                antonyms = data["antonyms"].orEmpty().splitPipedValues(),
                similarWords = data["similar_words"].orEmpty().splitPipedValues(),
                wordForms = data["word_forms"].orEmpty().splitPipedValues(),
                root = data["root"]?.ifBlank { null },
                exampleSentence = data["example_sentence"]?.ifBlank { null },
                exampleTranslation = data["example_translation"]?.ifBlank { null },
            )
        }

        return ImportedBook(
            metadata = ImportedBookMetadata(
                id = "imported-csv",
                title = "导入词书",
                description = "来自 CSV 文件的词书",
            ),
            words = words,
        )
    }

    private fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        line.forEach { char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result += current.toString()
        return result.map { it.trim() }
    }
}
