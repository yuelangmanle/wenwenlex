package com.yueliangmanle.danci.feature.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GoalSettingsScreen(
    state: GoalSettingsUiState,
    phaseNameInput: String,
    phaseTargetWordsInput: String,
    onPhaseNameChange: (String) -> Unit,
    onPhaseTargetWordsChange: (String) -> Unit,
    onDailyGoalDecreaseClick: () -> Unit,
    onDailyGoalIncreaseClick: () -> Unit,
    onWeeklyGoalDecreaseClick: () -> Unit,
    onWeeklyGoalIncreaseClick: () -> Unit,
    onSavePhaseClick: () -> Unit,
    onClearPhaseClick: () -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text("正在读取目标设置")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard(state = state)
        GoalAdjustCard(
            title = "每日目标",
            value = state.dailyGoal.toString(),
            supportingText = "今天已经完成 ${state.currentDayCompletedCount} 词。",
            decreaseLabel = "-5",
            increaseLabel = "+5",
            isWorking = state.isWorking,
            onDecreaseClick = onDailyGoalDecreaseClick,
            onIncreaseClick = onDailyGoalIncreaseClick,
        )
        GoalAdjustCard(
            title = "每周目标",
            value = state.weeklyGoal.toString(),
            supportingText = "本周已经推进 ${state.currentWeekCompletedCount} 词。",
            decreaseLabel = "-10",
            increaseLabel = "+10",
            isWorking = state.isWorking,
            onDecreaseClick = onWeeklyGoalDecreaseClick,
            onIncreaseClick = onWeeklyGoalIncreaseClick,
        )
        PhaseSettingsCard(
            phaseNameInput = phaseNameInput,
            phaseTargetWordsInput = phaseTargetWordsInput,
            phaseCompletedWords = state.phaseCompletedWords,
            isWorking = state.isWorking,
            onPhaseNameChange = onPhaseNameChange,
            onPhaseTargetWordsChange = onPhaseTargetWordsChange,
            onSavePhaseClick = onSavePhaseClick,
            onClearPhaseClick = onClearPhaseClick,
        )
        state.statusMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(state: GoalSettingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFCE7C8),
                            Color(0xFFE8EEF9),
                            Color(0xFFF3D8D2),
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "目标设置",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "把今天、本周和当前阶段的推进目标放在一起管理。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProgressMetric(
                    modifier = Modifier.weight(1f),
                    label = "连续学习",
                    value = "${state.currentStreakDays} 天",
                )
                ProgressMetric(
                    modifier = Modifier.weight(1f),
                    label = "最佳记录",
                    value = "${state.bestStreakDays} 天",
                )
            }
        }
    }
}

@Composable
private fun ProgressMetric(
    modifier: Modifier,
    label: String,
    value: String,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun GoalAdjustCard(
    title: String,
    value: String,
    supportingText: String,
    decreaseLabel: String,
    increaseLabel: String,
    isWorking: Boolean,
    onDecreaseClick: () -> Unit,
    onIncreaseClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDecreaseClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isWorking,
                ) {
                    Text(decreaseLabel)
                }
                Button(
                    onClick = onIncreaseClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isWorking,
                ) {
                    Text("$increaseLabel 现在 $value")
                }
            }
        }
    }
}

@Composable
private fun PhaseSettingsCard(
    phaseNameInput: String,
    phaseTargetWordsInput: String,
    phaseCompletedWords: Int,
    isWorking: Boolean,
    onPhaseNameChange: (String) -> Unit,
    onPhaseTargetWordsChange: (String) -> Unit,
    onSavePhaseClick: () -> Unit,
    onClearPhaseClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "阶段目标",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "当前阶段已完成 $phaseCompletedWords 词，可以随时调整阶段名称和目标词量。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = phaseNameInput,
                onValueChange = onPhaseNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("阶段名称") },
                singleLine = true,
                enabled = !isWorking,
            )
            OutlinedTextField(
                value = phaseTargetWordsInput,
                onValueChange = onPhaseTargetWordsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("目标词量") },
                singleLine = true,
                enabled = !isWorking,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onSavePhaseClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isWorking,
                ) {
                    Text("保存阶段")
                }
                OutlinedButton(
                    onClick = onClearPhaseClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isWorking,
                ) {
                    Text("清空阶段")
                }
            }
        }
    }
}
