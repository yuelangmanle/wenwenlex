package com.yueliangmanle.danci.core.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportDiagnosisEngineTest {
    private val engine = ImportDiagnosisEngine()

    @Test
    fun diagnose_marksCommaAndWhitespaceAsMinorIssues() {
        val diagnosis = engine.diagnose(
            listOf(
                XlsxSheetData(
                    name = "Sheet1",
                    rows = listOf(
                        listOf(" abandon ", "放弃，抛弃 "),
                    ),
                ),
            ),
        )

        assertEquals(ImportIssueSeverity.MINOR, diagnosis.issues.first().severity)
        assertTrue(diagnosis.autoFixableCount > 0)
        assertTrue(!diagnosis.requiresAiDecision)
    }

    @Test
    fun diagnose_marksMergedLikeBrokenLayoutAsSevereIssue() {
        val diagnosis = engine.diagnose(
            listOf(
                XlsxSheetData(
                    name = "Sheet1",
                    rows = listOf(
                        listOf("", "abandon", "放弃"),
                    ),
                ),
            ),
        )

        assertTrue(diagnosis.requiresAiDecision)
        assertTrue(diagnosis.issues.any { it.severity == ImportIssueSeverity.SEVERE })
    }
}
