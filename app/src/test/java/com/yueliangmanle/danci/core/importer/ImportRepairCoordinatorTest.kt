package com.yueliangmanle.danci.core.importer

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportRepairCoordinatorTest {
    @Test
    fun repair_appliesMinorFixesWithoutAiWhenNoSevereIssues() = runTest {
        val coordinator = ImportRepairCoordinator(
            diagnosisEngine = ImportDiagnosisEngine(),
            aiNormalize = { _, _ -> error("should not call ai") },
        )

        val result = coordinator.repair(
            workbook = listOf(
                XlsxSheetData(
                    name = "Sheet1",
                    rows = listOf(
                        listOf(" abandon ", "放弃，抛弃 "),
                    ),
                ),
            ),
            allowAi = false,
            runtimeSettings = null,
        )

        assertFalse(result.aiUsed)
        assertEquals("abandon", result.preview.rows.first().word)
        assertEquals(listOf("放弃", "抛弃"), result.preview.rows.first().meanings)
        assertTrue(result.autoRepairSummary.orEmpty().contains("自动修复"))
    }

    @Test
    fun repair_usesAiWhenSevereIssuesAndAllowed() = runTest {
        var aiCalled = false
        val coordinator = ImportRepairCoordinator(
            diagnosisEngine = ImportDiagnosisEngine(),
            aiNormalize = { _, _ ->
                aiCalled = true
                ImportPreview(
                    sheetName = "Sheet1",
                    rows = listOf(
                        ImportPreviewRow(
                            word = "abandon",
                            meanings = listOf("放弃"),
                            sourceLabel = "AI 修复",
                        ),
                    ),
                    totalRows = 1,
                    skippedRows = 0,
                    parserMode = "ai_repaired",
                    aiNormalizedCount = 1,
                    aiCompletedCount = 1,
                )
            },
        )

        val result = coordinator.repair(
            workbook = listOf(
                XlsxSheetData(
                    name = "Sheet1",
                    rows = listOf(
                        listOf("", "abandon", "放弃"),
                    ),
                ),
            ),
            allowAi = true,
            runtimeSettings = null,
        )

        assertTrue(aiCalled)
        assertTrue(result.aiUsed)
        assertEquals("AI 修复", result.preview.rows.first().sourceLabel)
    }
}
