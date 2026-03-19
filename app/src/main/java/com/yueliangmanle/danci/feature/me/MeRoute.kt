package com.yueliangmanle.danci.feature.me

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.buildAiProfileRepository
import com.yueliangmanle.danci.core.data.buildBackupRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.worker.DailyReminderScheduler
import kotlinx.coroutines.launch

const val AI_SETTINGS_ROUTE = "ai_settings"

@Composable
fun MeRoute(
    onOpenAiSettingsClick: () -> Unit = {},
    onOpenPronunciationSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember(context) {
        MeViewModel(
            settingsRepository = buildSettingsRepository(context),
            aiProfileRepository = buildAiProfileRepository(context),
            backupRepository = buildBackupRepository(context),
            studyRepository = RoomStudyRepository(buildDanciDatabase(context).studyDao()),
            reminderScheduler = DailyReminderScheduler(context),
        )
    }
    var state by remember {
        mutableStateOf(MeUiState(isLoading = true))
    }

    LaunchedEffect(viewModel) {
        state = viewModel.loadUiState()
    }

    fun launchAction(action: suspend MeViewModel.() -> MeUiState) {
        scope.launch {
            state = state.copy(isWorking = true, statusMessage = null)
            state = runCatching { viewModel.action() }.getOrElse { error ->
                state.copy(
                    isWorking = false,
                    statusMessage = error.message ?: "操作失败，请稍后重试。",
                )
            }
        }
    }

    MeScreen(
        state = state,
        onDailyGoalDecreaseClick = {
            launchAction { adjustDailyGoal(delta = -5) }
        },
        onDailyGoalIncreaseClick = {
            launchAction { adjustDailyGoal(delta = 5) }
        },
        onReminderEnabledChange = { enabled ->
            launchAction { updateReminderEnabled(enabled) }
        },
        onAdjustReminderTimeClick = {
            launchAction { shiftReminderTimeBy(minutes = 30) }
        },
        onExportBackupClick = {
            launchAction { exportBackup() }
        },
        onRestoreBackupClick = {
            launchAction { restoreLatestBackup() }
        },
        onOpenAiSettingsClick = onOpenAiSettingsClick,
        onOpenPronunciationSettingsClick = onOpenPronunciationSettingsClick,
    )
}
