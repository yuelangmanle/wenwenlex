package com.yueliangmanle.danci.core.diagnostics

import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsExporterTest {

    @Test
    fun exporter_writes_version_upgrade_and_recent_error_sections() = runTest {
        val exportDir = Files.createTempDirectory("diagnostics-export-test").toFile()
        val report = DiagnosticsReport(
            generatedAt = Instant.parse("2026-03-22T12:30:00Z"),
            appVersionName = "1.7",
            appVersionCode = 170,
            issueCount = 2,
            issues = listOf(
                DiagnosticsIssue(
                    code = "missing_backup",
                    severity = DiagnosticsSeverity.WARNING,
                    title = "缺少最近备份",
                    detail = "当前设备还没有本地备份文件。",
                ),
                DiagnosticsIssue(
                    code = "missing_default_ai_profile",
                    severity = DiagnosticsSeverity.ERROR,
                    title = "AI 默认档案丢失",
                    detail = "已开启 AI，但默认档案不存在。",
                ),
            ),
            upgradeHealth = mapOf(
                "status" to "needs_attention",
                "checked_at" to "2026-03-22T12:30:00Z",
            ),
            summaryLines = listOf(
                "版本：1.7 (170)",
                "问题数：2",
                "状态：needs_attention",
            ),
        )

        val archive = DiagnosticsExporter(exportDir).export(report)

        assertTrue(archive.exists())

        val entryNames = mutableListOf<String>()
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryNames += entry.name
                zip.closeEntry()
            }
        }

        assertTrue(entryNames.contains("diagnostics.json"))
        assertTrue(entryNames.contains("summary.txt"))
    }
}
