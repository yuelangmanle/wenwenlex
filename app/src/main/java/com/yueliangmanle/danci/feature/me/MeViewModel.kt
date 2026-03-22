package com.yueliangmanle.danci.feature.me

import com.yueliangmanle.danci.core.data.BackupRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.data.AiProfileRepository
import com.yueliangmanle.danci.core.goal.GoalProgressTracker
import com.yueliangmanle.danci.core.worker.DailyReminderScheduler
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MeUiState(
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val dailyGoal: Int = 20,
    val weeklyGoal: Int = 70,
    val streakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val weeklyActiveDays: Int = 0,
    val phaseName: String? = null,
    val phaseTargetWords: Int = 0,
    val phaseCompletedWords: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderTimeLabel: String = "21:00",
    val backupSummary: String = "还没有本地备份",
    val canRestoreBackup: Boolean = false,
    val aiEnabled: Boolean = false,
    val aiModel: String = "gpt-5-mini",
    val diagnosticsSummary: String = "最近还没有诊断记录",
    val statusMessage: String? = null,
)

class MeViewModel(
    private val settingsRepository: SettingsRepository,
    private val aiProfileRepository: AiProfileRepository,
    private val backupRepository: BackupRepository,
    private val studyRepository: StudyRepository,
    private val reminderScheduler: DailyReminderScheduler,
    private val goalProgressTracker: GoalProgressTracker = GoalProgressTracker(),
    private val nowProvider: () -> Instant = { Instant.now() },
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun loadUiState(statusMessage: String? = null): MeUiState {
        val settings = settingsRepository.getSettings()
        val defaultProfile = if (settings.defaultAiProfileId != null) {
            aiProfileRepository.getProfile(settings.defaultAiProfileId)
        } else {
            null
        }
        val aiMemorySummary = studyRepository.loadAiMemorySummary(planLimit = 50)
        val learningRecords = studyRepository.getAllLearningRecords()
        val events = studyRepository.getAllStudyEvents()
        val activeDates = events
            .map { event -> event.happenedAt.atZone(zoneId).toLocalDate() }
            .distinct()
            .sorted()
        val goalProgress = goalProgressTracker.build(
            records = learningRecords,
            settings = settings,
            now = nowProvider(),
            studyEvents = events,
        )
        val latestBackup = backupRepository.latestBackupFile()

        return MeUiState(
            dailyGoal = settings.dailyGoal,
            weeklyGoal = settings.weeklyGoal,
            streakDays = goalProgress.currentStreakDays,
            bestStreakDays = goalProgress.bestStreakDays,
            weeklyActiveDays = calculateWeeklyActiveDays(activeDates),
            phaseName = goalProgress.phaseName,
            phaseTargetWords = goalProgress.phaseTargetWords,
            phaseCompletedWords = goalProgress.phaseCompletedWords,
            reminderEnabled = settings.reminderEnabled,
            reminderTimeLabel = formatReminderTime(settings.reminderHour, settings.reminderMinute),
            backupSummary = latestBackup.toBackupSummary(zoneId),
            canRestoreBackup = latestBackup != null,
            aiEnabled = settings.aiEnabled,
            aiModel = defaultProfile?.let { "${it.name} · ${it.model}" } ?: settings.aiModel,
            diagnosticsSummary = aiMemorySummary.upgradeHealth.toDiagnosticsSummary(zoneId),
            statusMessage = statusMessage,
        )
    }

    suspend fun adjustDailyGoal(delta: Int): MeUiState {
        val settings = settingsRepository.getSettings()
        settingsRepository.updateDailyGoal(settings.dailyGoal + delta)
        return loadUiState(
            statusMessage = "每日目标已更新为 ${settingsRepository.getSettings().dailyGoal} 词。",
        )
    }

    suspend fun updateReminderEnabled(enabled: Boolean): MeUiState {
        settingsRepository.updateReminderEnabled(enabled)
        val settings = settingsRepository.getSettings()
        reminderScheduler.sync(settings)
        return loadUiState(
            statusMessage = if (enabled) {
                "每日提醒已开启，会在 ${formatReminderTime(settings.reminderHour, settings.reminderMinute)} 提醒。"
            } else {
                "每日提醒已关闭。"
            },
        )
    }

    suspend fun shiftReminderTimeBy(minutes: Int = 30): MeUiState {
        val settings = settingsRepository.getSettings()
        val totalMinutes = (
            (
                settings.reminderHour * 60 +
                    settings.reminderMinute +
                    minutes
                ) % (24 * 60) + (24 * 60)
            ) % (24 * 60)
        val hour = totalMinutes / 60
        val minute = totalMinutes % 60
        settingsRepository.updateReminderTime(hour, minute)
        val updatedSettings = settingsRepository.getSettings()
        reminderScheduler.sync(updatedSettings)
        return loadUiState(
            statusMessage = "提醒时间已调整到 ${formatReminderTime(hour, minute)}。",
        )
    }

    suspend fun exportBackup(): MeUiState {
        val result = backupRepository.exportToLocalFile()
        return loadUiState(
            statusMessage = "已导出本地备份：${result.file.name}",
        )
    }

    suspend fun restoreLatestBackup(): MeUiState {
        val result = backupRepository.restoreLatestBackup()
        reminderScheduler.sync(settingsRepository.getSettings())
        return loadUiState(
            statusMessage = "已恢复最近一次备份：${result.file.name}",
        )
    }

    private fun calculateWeeklyActiveDays(activeDates: List<LocalDate>): Int {
        if (activeDates.isEmpty()) {
            return 0
        }
        val start = LocalDate.ofInstant(nowProvider(), zoneId).minusDays(6)
        return activeDates.count { it >= start }
    }
}

private fun formatReminderTime(
    hour: Int,
    minute: Int,
): String = "%02d:%02d".format(hour, minute)

private fun File?.toBackupSummary(zoneId: ZoneId): String =
    this?.let { file ->
        val timestamp = Instant.ofEpochMilli(file.lastModified())
            .atZone(zoneId)
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        "最近备份：$timestamp"
    } ?: "还没有本地备份"

private fun Map<String, String>.toDiagnosticsSummary(zoneId: ZoneId): String {
    val status = this["status"]
    val checkedAt = this["checked_at"]?.let(Instant::parse)
    val checkedAtLabel = checkedAt
        ?.atZone(zoneId)
        ?.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    val summary = this["summary"]
    return when {
        !summary.isNullOrBlank() && !checkedAtLabel.isNullOrBlank() -> "$summary（$checkedAtLabel）"
        !summary.isNullOrBlank() -> summary
        status == "healthy" -> "最近一次诊断未发现关键问题。"
        status == "needs_attention" -> "最近一次诊断发现需要处理的问题。"
        else -> "最近还没有诊断记录"
    }
}
