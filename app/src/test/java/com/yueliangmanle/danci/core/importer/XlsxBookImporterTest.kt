package com.yueliangmanle.danci.core.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxBookImporterTest {
    private val importer = XlsxBookImporter()

    @Test
    fun previewSkipsHeaderAndSplitsChineseCommaMeanings() {
        val preview = importer.preview(
            XlsxSheetData(
                name = "Sheet1",
                rows = listOf(
                    listOf("单词", "中文释义"),
                    listOf("abandon", "放弃，遗弃"),
                    listOf("brief", "简短的,摘要"),
                ),
            ),
        )

        assertEquals(2, preview.rows.size)
        assertEquals(listOf("放弃", "遗弃"), preview.rows.first().meanings)
        assertEquals(listOf("简短的", "摘要"), preview.rows.last().meanings)
    }

    @Test
    fun previewWarnsWhenNoRowsCanBeParsed() {
        val preview = importer.preview(
            XlsxSheetData(
                name = "Sheet1",
                rows = listOf(
                    listOf("备注", "说明"),
                    listOf("2026-03-19", "无有效词条"),
                ),
            ),
        )

        assertTrue(preview.rows.isEmpty())
        assertTrue(preview.warningMessage.orEmpty().contains("AI 适配"))
    }
}
