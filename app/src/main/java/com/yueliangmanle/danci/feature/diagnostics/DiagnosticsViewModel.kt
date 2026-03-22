package com.yueliangmanle.danci.feature.diagnostics

import com.yueliangmanle.danci.core.diagnostics.DiagnosticsIssue
import com.yueliangmanle.danci.core.diagnostics.DiagnosticsRepository
import com.yueliangmanle.danci.core.diagnostics.DiagnosticsSeverity
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DiagnosticsIssueUiModel(
    val severityLabel: String,
    val title: String,
    val detail: String,
)

data class DiagnosticsDetailUiModel(
    val label: String,
    val value: String,
)

data class DiagnosticsUiState(
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val appVersionLabel: String = "",
    val checkedAtLabel: String = "",
    val statusTitle: String = "诊断中心",
    val summary: String = "",
    val issues: List<DiagnosticsIssueUiModel> = emptyList(),
    val details: List<DiagnosticsDetailUiModel> = emptyList(),
    val statusMessage: String? = null,
)

class DiagnosticsViewModel(
    private val diagnosticsRepository: DiagnosticsRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun loadUiState(statusMessage: String? = null): DiagnosticsUiState {
        val report = diagnosticsRepository.buildReport()
        val snapshot = requireNotNull(report.snapshot)
        return DiagnosticsUiState(
            appVersionLabel = "版本 ${report.appVersionName} (${report.appVersionCode})",
            checkedAtLabel = report.generatedAt.atZone(zoneId).format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
            statusTitle = if (report.issueCount == 0) "状态正常" else "需要处理",
            summary = report.upgradeHealth["summary"] ?: "最近还没有诊断摘要。",
            issues = report.issues.map(DiagnosticsIssue::asUiModel),
            details = listOf(
                DiagnosticsDetailUiModel("学习记录", snapshot.learningRecordCount.toString()),
                DiagnosticsDetailUiModel("学习事件", snapshot.studyEventCount.toString()),
                DiagnosticsDetailUiModel("词书数量", snapshot.bookCount.toString()),
                DiagnosticsDetailUiModel("AI 档案", snapshot.aiProfileCount.toString()),
                DiagnosticsDetailUiModel("已装语音包", snapshot.installedVoicePackCount.toString()),
                DiagnosticsDetailUiModel("最近备份", snapshot.latestBackupName ?: "无"),
            ),
            statusMessage = statusMessage,
        )
    }

    suspend fun exportDiagnostics(): DiagnosticsUiState {
        val report = diagnosticsRepository.buildReport()
        val archive = diagnosticsRepository.exportReport(report)
        return loadUiState(statusMessage = "已导出诊断包：${archive.name}")
    }
}

private fun DiagnosticsIssue.asUiModel(): DiagnosticsIssueUiModel =
    DiagnosticsIssueUiModel(
        severityLabel = when (severity) {
            DiagnosticsSeverity.INFO -> "提示"
            DiagnosticsSeverity.WARNING -> "提醒"
            DiagnosticsSeverity.ERROR -> "错误"
        },
        title = title,
        detail = detail,
    )
