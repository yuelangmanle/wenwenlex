package com.yueliangmanle.danci.core.diagnostics

class DataIntegrityVerifier {
    fun verify(snapshot: DataIntegritySnapshot): UpgradeHealthReport {
        val issues = buildList {
            if (snapshot.settings.dailyGoal <= 0 || snapshot.settings.weeklyGoal <= 0) {
                add(
                    DiagnosticsIssue(
                        code = "missing_goal_settings",
                        severity = DiagnosticsSeverity.ERROR,
                        title = "目标设置异常",
                        detail = "每日目标或每周目标缺失，建议重新检查目标设置页。",
                    ),
                )
            }
            if (snapshot.learningRecordCount <= 0) {
                add(
                    DiagnosticsIssue(
                        code = "empty_learning_records",
                        severity = DiagnosticsSeverity.WARNING,
                        title = "学习记录为空",
                        detail = "当前还没有有效的学习记录，首页与统计页可能缺少真实进度。",
                    ),
                )
            }
            if (snapshot.studyEventCount <= 0) {
                add(
                    DiagnosticsIssue(
                        code = "empty_study_events",
                        severity = DiagnosticsSeverity.WARNING,
                        title = "学习事件为空",
                        detail = "当前没有历史学习事件，连续学习与阶段推进无法完整回放。",
                    ),
                )
            }
            if (snapshot.bookCount <= 0) {
                add(
                    DiagnosticsIssue(
                        code = "missing_books",
                        severity = DiagnosticsSeverity.WARNING,
                        title = "词书数据为空",
                        detail = "当前数据库里没有可用词书，建议检查内置词书同步或导入流程。",
                    ),
                )
            }
            if (!snapshot.hasBackup) {
                add(
                    DiagnosticsIssue(
                        code = "missing_backup",
                        severity = DiagnosticsSeverity.WARNING,
                        title = "缺少本地备份",
                        detail = "当前设备还没有最近备份，升级前建议先导出一份本地备份。",
                    ),
                )
            }
            if (
                snapshot.settings.aiEnabled &&
                (
                    snapshot.settings.defaultAiProfileId.isNullOrBlank() ||
                        snapshot.aiProfileCount <= 0
                    )
            ) {
                add(
                    DiagnosticsIssue(
                        code = "missing_default_ai_profile",
                        severity = DiagnosticsSeverity.ERROR,
                        title = "AI 默认档案丢失",
                        detail = "已开启 AI，但默认档案不存在或档案列表为空，相关 AI 功能会失败。",
                    ),
                )
            }
            if (
                !snapshot.settings.activeVoicePackId.isNullOrBlank() &&
                snapshot.installedVoicePackCount <= 0
            ) {
                add(
                    DiagnosticsIssue(
                        code = "missing_active_voice_pack",
                        severity = DiagnosticsSeverity.WARNING,
                        title = "激活语音包不可用",
                        detail = "设置中存在激活语音包，但当前设备没有已安装可用的语音包。",
                    ),
                )
            }
        }

        val status = if (issues.isEmpty()) "healthy" else "needs_attention"
        val summary = if (issues.isEmpty()) {
            "最近一次完整性检查未发现关键问题。"
        } else {
            "最近一次完整性检查发现 ${issues.size} 个需要处理的问题。"
        }

        return UpgradeHealthReport(
            checkedAt = snapshot.checkedAt,
            status = status,
            summary = summary,
            issues = issues,
        )
    }
}
