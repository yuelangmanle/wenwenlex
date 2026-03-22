package com.yueliangmanle.danci.core.diagnostics

import com.yueliangmanle.danci.core.data.AppSettings
import java.time.Instant

data class DataIntegritySnapshot(
    val checkedAt: Instant,
    val settings: AppSettings,
    val learningRecordCount: Int,
    val studyEventCount: Int,
    val bookCount: Int,
    val aiProfileCount: Int,
    val installedVoicePackCount: Int,
    val hasBackup: Boolean,
    val latestBackupName: String? = null,
    val latestBackupAt: Instant? = null,
    val upgradeHealth: Map<String, String> = emptyMap(),
)

enum class DiagnosticsSeverity {
    INFO,
    WARNING,
    ERROR,
}

data class DiagnosticsIssue(
    val code: String,
    val severity: DiagnosticsSeverity,
    val title: String,
    val detail: String,
)

data class UpgradeHealthReport(
    val checkedAt: Instant,
    val status: String,
    val summary: String,
    val issues: List<DiagnosticsIssue>,
)

data class DiagnosticsReport(
    val generatedAt: Instant,
    val appVersionName: String,
    val appVersionCode: Int,
    val issueCount: Int,
    val issues: List<DiagnosticsIssue>,
    val upgradeHealth: Map<String, String>,
    val summaryLines: List<String>,
    val snapshot: DataIntegritySnapshot? = null,
)
