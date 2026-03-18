package com.yueliangmanle.danci.core.importer

import java.io.InputStream
import java.util.Locale

data class ImportPreviewRow(
    val word: String,
    val meanings: List<String>,
    val phoneticUk: String? = null,
    val phoneticUs: String? = null,
    val sourceLabel: String = "本地解析",
)

data class ImportPreview(
    val sheetName: String,
    val rows: List<ImportPreviewRow>,
    val totalRows: Int,
    val skippedRows: Int,
    val parserMode: String = "strict",
    val aiNormalizedCount: Int = 0,
    val aiCompletedCount: Int = 0,
    val warningMessage: String? = null,
)

class XlsxBookImporter(
    private val tableReader: XlsxXmlTableReader = XlsxXmlTableReader(),
) : BookImportParser {
    override fun parse(inputStream: InputStream): ImportedBook =
        preview(inputStream).toImportedBook()

    fun preview(inputStream: InputStream): ImportPreview {
        val sheets = tableReader.readWorkbook(inputStream)
        val selected = sheets.firstOrNull { it.rows.isNotEmpty() }
            ?: error("Excel 文件中没有可读取的工作表")
        return preview(selected)
    }

    fun preview(sheet: XlsxSheetData): ImportPreview {
        val rows = mutableListOf<ImportPreviewRow>()
        var skipped = 0
        sheet.rows.forEachIndexed { index, row ->
            val parsed = parseRow(row)
            if (parsed == null) {
                skipped += 1
            } else {
                rows += parsed
            }
        }
        return ImportPreview(
            sheetName = sheet.name,
            rows = rows,
            totalRows = sheet.rows.size,
            skippedRows = skipped,
            warningMessage = when {
                rows.isEmpty() -> "没有在前两列里识别到有效的单词和中文释义，可以尝试 AI 适配。"
                skipped > rows.size -> "跳过的行较多，如果原表结构不规整，建议尝试 AI 适配。"
                else -> null
            },
        )
    }

    fun toImportedBook(
        preview: ImportPreview,
        title: String = "Excel 导入词书",
        id: String = "imported-xlsx",
    ): ImportedBook =
        ImportedBook(
            metadata = ImportedBookMetadata(
                id = id,
                title = title,
                description = "来自 Excel 文件的词书",
            ),
            words = preview.rows.map { row ->
                ImportedWord(
                    text = row.word,
                    meanings = row.meanings,
                    phoneticUk = row.phoneticUk,
                    phoneticUs = row.phoneticUs,
                    phonetic = row.phoneticUk ?: row.phoneticUs,
                )
            },
        )

    private fun ImportPreview.toImportedBook(): ImportedBook = toImportedBook(preview = this)

    private fun parseRow(row: List<String>): ImportPreviewRow? {
        if (row.isEmpty()) return null
        val first = row.getOrNull(0).orEmpty().trim()
        val second = row.getOrNull(1).orEmpty().trim()
        if (first.isBlank()) return null
        if (looksLikeHeader(first, second)) return null
        if (!looksLikeWord(first)) return null
        val meanings = normalizeMeanings(second)
        if (meanings.isEmpty()) return null
        return ImportPreviewRow(
            word = first,
            meanings = meanings,
        )
    }

    private fun looksLikeHeader(
        first: String,
        second: String,
    ): Boolean {
        val firstLower = first.lowercase(Locale.ROOT)
        val secondLower = second.lowercase(Locale.ROOT)
        return firstLower in setOf("word", "words", "单词", "英文", "english") ||
            secondLower in setOf("meaning", "meanings", "translation", "中文", "释义")
    }

    private fun looksLikeWord(value: String): Boolean =
        value.matches(Regex("[A-Za-z][A-Za-z\\- '\\u2019]*"))

    internal fun normalizeMeanings(raw: String): List<String> =
        raw.replace('，', ',')
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
}
