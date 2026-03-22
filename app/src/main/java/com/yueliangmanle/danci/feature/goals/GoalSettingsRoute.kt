package com.yueliangmanle.danci.feature.goals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import kotlinx.coroutines.launch

const val GOAL_SETTINGS_ROUTE = "goal_settings"

@Composable
fun GoalSettingsRoute() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember(context) {
        GoalSettingsViewModel(
            settingsRepository = buildSettingsRepository(context),
            studyRepository = RoomStudyRepository(buildDanciDatabase(context).studyDao()),
        )
    }
    var state by remember {
        mutableStateOf(GoalSettingsUiState(isLoading = true))
    }
    var phaseNameInput by rememberSaveable { mutableStateOf("") }
    var phaseTargetWordsInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        state = viewModel.loadUiState()
    }

    LaunchedEffect(state.phaseName, state.phaseTargetWords) {
        phaseNameInput = state.phaseName
        phaseTargetWordsInput = state.phaseTargetWords.takeIf { it > 0 }?.toString().orEmpty()
    }

    fun launchAction(action: suspend GoalSettingsViewModel.() -> GoalSettingsUiState) {
        scope.launch {
            state = state.copy(isWorking = true, statusMessage = null)
            state = runCatching { viewModel.action() }.getOrElse { error ->
                state.copy(
                    isWorking = false,
                    statusMessage = error.message ?: "目标设置更新失败，请稍后重试。",
                )
            }
        }
    }

    GoalSettingsScreen(
        state = state,
        phaseNameInput = phaseNameInput,
        phaseTargetWordsInput = phaseTargetWordsInput,
        onPhaseNameChange = { phaseNameInput = it },
        onPhaseTargetWordsChange = { input ->
            phaseTargetWordsInput = input.filter(Char::isDigit)
        },
        onDailyGoalDecreaseClick = {
            launchAction { adjustDailyGoal(delta = -5) }
        },
        onDailyGoalIncreaseClick = {
            launchAction { adjustDailyGoal(delta = 5) }
        },
        onWeeklyGoalDecreaseClick = {
            launchAction { adjustWeeklyGoal(delta = -10) }
        },
        onWeeklyGoalIncreaseClick = {
            launchAction { adjustWeeklyGoal(delta = 10) }
        },
        onSavePhaseClick = {
            launchAction {
                savePhaseSettings(
                    phaseName = phaseNameInput,
                    phaseTargetWords = phaseTargetWordsInput.toIntOrNull() ?: 0,
                )
            }
        },
        onClearPhaseClick = {
            launchAction { clearPhaseSettings() }
        },
    )
}
