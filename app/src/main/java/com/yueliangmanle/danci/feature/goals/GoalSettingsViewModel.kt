package com.yueliangmanle.danci.feature.goals

import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.goal.GoalProgressTracker
import java.time.Instant

data class GoalSettingsUiState(
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val dailyGoal: Int = 20,
    val weeklyGoal: Int = 70,
    val phaseName: String = "",
    val phaseTargetWords: Int = 0,
    val currentDayCompletedCount: Int = 0,
    val currentWeekCompletedCount: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val phaseCompletedWords: Int = 0,
    val statusMessage: String? = null,
)

class GoalSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val studyRepository: StudyRepository,
    private val goalProgressTracker: GoalProgressTracker = GoalProgressTracker(),
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    suspend fun loadUiState(statusMessage: String? = null): GoalSettingsUiState {
        val settings = settingsRepository.getSettings()
        val records = studyRepository.getAllLearningRecords()
        val studyEvents = studyRepository.getAllStudyEvents()
        val progress = goalProgressTracker.build(
            records = records,
            settings = settings,
            now = nowProvider(),
            studyEvents = studyEvents,
        )

        return GoalSettingsUiState(
            dailyGoal = settings.dailyGoal,
            weeklyGoal = settings.weeklyGoal,
            phaseName = settings.phaseName.orEmpty(),
            phaseTargetWords = settings.phaseTargetWords,
            currentDayCompletedCount = progress.currentDayCompletedCount,
            currentWeekCompletedCount = progress.currentWeekCompletedCount,
            currentStreakDays = progress.currentStreakDays,
            bestStreakDays = progress.bestStreakDays,
            phaseCompletedWords = progress.phaseCompletedWords,
            statusMessage = statusMessage,
        )
    }

    suspend fun adjustDailyGoal(delta: Int): GoalSettingsUiState {
        val settings = settingsRepository.getSettings()
        settingsRepository.updateDailyGoal(settings.dailyGoal + delta)
        return loadUiState(
            statusMessage = "每日目标已更新为 ${settingsRepository.getSettings().dailyGoal} 词。",
        )
    }

    suspend fun adjustWeeklyGoal(delta: Int): GoalSettingsUiState {
        val settings = settingsRepository.getSettings()
        settingsRepository.updateWeeklyGoal(settings.weeklyGoal + delta)
        return loadUiState(
            statusMessage = "每周目标已更新为 ${settingsRepository.getSettings().weeklyGoal} 词。",
        )
    }

    suspend fun savePhaseSettings(
        phaseName: String,
        phaseTargetWords: Int,
    ): GoalSettingsUiState {
        settingsRepository.updatePhaseName(phaseName.trim().takeIf(String::isNotBlank))
        settingsRepository.updatePhaseTargetWords(phaseTargetWords)
        return loadUiState(
            statusMessage = if (phaseName.isBlank() && phaseTargetWords <= 0) {
                "阶段目标已清空。"
            } else {
                "阶段目标已更新。"
            },
        )
    }

    suspend fun clearPhaseSettings(): GoalSettingsUiState {
        settingsRepository.updatePhaseName(null)
        settingsRepository.updatePhaseTargetWords(0)
        return loadUiState(statusMessage = "阶段目标已清空。")
    }
}
