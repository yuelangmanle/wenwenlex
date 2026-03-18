package com.yueliangmanle.danci.feature.me

import com.yueliangmanle.danci.core.data.BackupRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.StudyRepository
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
    val streakDays: Int = 0,
    val weeklyActiveDays: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderTimeLabel: String = "21:00",
    val backupSummary: String = "还没有本地备份",
    val canRestoreBackup: Boolean = false,
    val aiEnabled: Boolean = false,
    val aiModel: String = "gpt-5-mini",
    val statusMessage: String? = null,
)

class MeViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val studyRepository: StudyRepository,
    private val reminderScheduler: DailyReminderScheduler,
    private val nowProvider: () -> Instant = { Instant.now() },
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun loadUiState(statusMessage: String? = null): MeUiState {
        val settings = settingsRepository.getSettings()
        val events = studyRepository.getAllStudyEvents()
        val activeDates = events
            .map { event -> event.happenedAt.atZone(zoneId).toLocalDate() }
            .distinct()
            .sorted()
        val latestBackup = backupRepository.latestBackupFile()

        return MeUiState(
            dailyGoal = settings.dailyGoal,
            streakDays = calculateStreakDays(activeDates),
            weeklyActiveDays = calculateWeeklyActiveDays(activeDates),
            reminderEnabled = settings.reminderEnabled,
            reminderTimeLabel = formatReminderTime(settings.reminderHour, settings.reminderMinute),
            backupSummary = latestBackup.toBackupSummary(zoneId),
            canRestoreBackup = latestBackup != null,
            aiEnabled = settings.aiEnabled,
            aiModel = settings.aiModel,
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

    private fun calculateStreakDays(activeDates: List<LocalDate>): Int {
        if (activeDates.isEmpty()) {
            return 0
        }
        val activeDateSet = activeDates.toSet()
        var cursor = LocalDate.ofInstant(nowProvider(), zoneId)
        var streak = 0
        while (activeDateSet.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
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
