package com.yueliangmanle.danci.feature.me

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MeScreen(
    state: MeUiState,
    onDailyGoalDecreaseClick: () -> Unit = {},
    onDailyGoalIncreaseClick: () -> Unit = {},
    onReminderEnabledChange: (Boolean) -> Unit,
    onAdjustReminderTimeClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onOpenAiSettingsClick: () -> Unit,
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
                Text("正在读取文文Lex 设置")
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
        DailyGoalCard(
            dailyGoal = state.dailyGoal,
            isWorking = state.isWorking,
            onDecreaseClick = onDailyGoalDecreaseClick,
            onIncreaseClick = onDailyGoalIncreaseClick,
        )
        ReminderCard(
            enabled = state.reminderEnabled,
            timeLabel = state.reminderTimeLabel,
            isWorking = state.isWorking,
            onReminderEnabledChange = onReminderEnabledChange,
            onAdjustReminderTimeClick = onAdjustReminderTimeClick,
        )
        BackupCard(
            summary = state.backupSummary,
            canRestoreBackup = state.canRestoreBackup,
            isWorking = state.isWorking,
            onExportBackupClick = onExportBackupClick,
            onRestoreBackupClick = onRestoreBackupClick,
        )
        AiSettingsCard(
            enabled = state.aiEnabled,
            model = state.aiModel,
            isWorking = state.isWorking,
            onOpenAiSettingsClick = onOpenAiSettingsClick,
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
private fun HeroCard(state: MeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEAF4FF),
                            Color(0xFFFBE6C8),
                            Color(0xFFF3D8D2),
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "文文Lex 控制台",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "把每日目标、提醒、备份恢复和 AI 配置收在一个地方。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "连续学习",
                    value = "${state.streakDays} 天",
                )
                HeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "本周打卡",
                    value = "${state.weeklyActiveDays}/7",
                )
            }
            LinearProgressIndicator(
                progress = { state.weeklyActiveDays.coerceIn(0, 7) / 7f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeroMetric(
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
private fun DailyGoalCard(
    dailyGoal: Int,
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
                text = "每日目标",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "现在每天计划学习 $dailyGoal 词。",
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
                    enabled = !isWorking && dailyGoal > 5,
                ) {
                    Text("-5")
                }
                Button(
                    onClick = onIncreaseClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isWorking,
                ) {
                    Text("+5")
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    enabled: Boolean,
    timeLabel: String,
    isWorking: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    onAdjustReminderTimeClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "每日提醒",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (enabled) {
                            "当前提醒时间 $timeLabel"
                        } else {
                            "关闭后不会发送每日提醒通知"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onReminderEnabledChange,
                    enabled = !isWorking,
                )
            }
            OutlinedButton(
                onClick = onAdjustReminderTimeClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isWorking,
            ) {
                Text("顺延 30 分钟（当前 $timeLabel）")
            }
        }
    }
}

@Composable
private fun BackupCard(
    summary: String,
    canRestoreBackup: Boolean,
    isWorking: Boolean,
    onExportBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "备份与恢复",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onExportBackupClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isWorking,
                ) {
                    Text("导出备份")
                }
                OutlinedButton(
                    onClick = onRestoreBackupClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isWorking && canRestoreBackup,
                ) {
                    Text("恢复最近备份")
                }
            }
        }
    }
}

@Composable
private fun AiSettingsCard(
    enabled: Boolean,
    model: String,
    isWorking: Boolean,
    onOpenAiSettingsClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "AI 设置",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (enabled) {
                    "当前模型：$model"
                } else {
                    "AI 目前关闭，核心学习仍然可离线使用。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onOpenAiSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isWorking,
            ) {
                Text("打开 AI 设置")
            }
        }
    }
}
