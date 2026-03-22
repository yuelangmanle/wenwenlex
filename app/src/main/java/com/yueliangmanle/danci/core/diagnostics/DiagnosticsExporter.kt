package com.yueliangmanle.danci.core.diagnostics

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

class DiagnosticsExporter(
    private val exportDir: File,
) {
    suspend fun export(report: DiagnosticsReport): File {
        exportDir.mkdirs()
        val archive = exportDir.resolve(
            "wenwenlex-diagnostics-${report.generatedAt.toEpochMilli()}.zip",
        )

        ZipOutputStream(archive.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("diagnostics.json"))
            zip.write(report.toJson().toString(2).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("summary.txt"))
            zip.write(report.toSummaryText().toByteArray())
            zip.closeEntry()
        }

        return archive
    }
}

private fun DiagnosticsReport.toJson(): JSONObject =
    JSONObject()
        .put("generated_at", generatedAt.toString())
        .put("app_version_name", appVersionName)
        .put("app_version_code", appVersionCode)
        .put("issue_count", issueCount)
        .put("upgrade_health", JSONObject(upgradeHealth))
        .put(
            "issues",
            JSONArray(
                issues.map { issue ->
                    JSONObject()
                        .put("code", issue.code)
                        .put("severity", issue.severity.name)
                        .put("title", issue.title)
                        .put("detail", issue.detail)
                },
            ),
        )
        .put("summary_lines", JSONArray(summaryLines))
        .put(
            "snapshot",
            snapshot?.let { snapshotValue ->
                JSONObject()
                    .put("checked_at", snapshotValue.checkedAt.toString())
                    .put("daily_goal", snapshotValue.settings.dailyGoal)
                    .put("weekly_goal", snapshotValue.settings.weeklyGoal)
                    .put("phase_name", snapshotValue.settings.phaseName)
                    .put("phase_target_words", snapshotValue.settings.phaseTargetWords)
                    .put("active_book_id", snapshotValue.settings.activeBookId)
                    .put("ai_enabled", snapshotValue.settings.aiEnabled)
                    .put("default_ai_profile_id", snapshotValue.settings.defaultAiProfileId)
                    .put("active_voice_pack_id", snapshotValue.settings.activeVoicePackId)
                    .put("learning_record_count", snapshotValue.learningRecordCount)
                    .put("study_event_count", snapshotValue.studyEventCount)
                    .put("book_count", snapshotValue.bookCount)
                    .put("ai_profile_count", snapshotValue.aiProfileCount)
                    .put("installed_voice_pack_count", snapshotValue.installedVoicePackCount)
                    .put("has_backup", snapshotValue.hasBackup)
                    .put("latest_backup_name", snapshotValue.latestBackupName)
                    .put("latest_backup_at", snapshotValue.latestBackupAt?.toString())
            } ?: JSONObject.NULL,
        )

private fun DiagnosticsReport.toSummaryText(): String =
    buildString {
        summaryLines.forEach { line ->
            appendLine(line)
        }
        appendLine()
        appendLine("问题明细：")
        if (issues.isEmpty()) {
            appendLine("- 当前未发现关键问题")
        } else {
            issues.forEach { issue ->
                appendLine("- [${issue.severity.name}] ${issue.title}")
                appendLine("  ${issue.detail}")
            }
        }
    }
