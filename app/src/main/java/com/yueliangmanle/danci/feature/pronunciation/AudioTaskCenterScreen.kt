package com.yueliangmanle.danci.feature.pronunciation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AudioTaskCenterScreen(
    state: AudioTaskCenterUiState,
    onSelectScope: (String) -> Unit,
    onCreateJob: (String) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("音频任务中心", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "在后台批量创建词典缓存、云端 TTS 缓存和本地离线语音生成任务。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text("正在同步音频任务…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("任务范围", style = MaterialTheme.typography.titleMedium)
                state.scopeOptions.forEach { option ->
                    if (option.isSelected) {
                        Button(
                            onClick = { onSelectScope(option.scopeType) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${option.label} · ${option.countLabel}")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectScope(option.scopeType) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${option.label} · ${option.countLabel}")
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("创建任务", style = MaterialTheme.typography.titleMedium)
                state.createOptions.forEach { option ->
                    OutlinedButton(
                        onClick = { onCreateJob(option.jobType) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(option.label)
                            Text(
                                option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("后台任务", style = MaterialTheme.typography.titleMedium)
                if (state.jobs.isEmpty()) {
                    Text("当前还没有音频后台任务。", style = MaterialTheme.typography.bodyMedium)
                } else {
                    state.jobs.forEach { job ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(job.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${job.scopeLabel} · ${job.progressLabel} · ${job.statusLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                job.errorMessage?.let { message ->
                                    Text(
                                        message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (job.canPause) {
                                        OutlinedButton(
                                            onClick = { onPause(job.id) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("暂停")
                                        }
                                    }
                                    if (job.canResume) {
                                        OutlinedButton(
                                            onClick = { onResume(job.id) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("继续")
                                        }
                                    }
                                    if (job.canCancel) {
                                        OutlinedButton(
                                            onClick = { onCancel(job.id) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("取消")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        state.statusMessage?.let { message ->
            AudioTaskMessageCard(message = message, isError = false)
        }
        state.errorMessage?.let { message ->
            AudioTaskMessageCard(message = message, isError = true)
        }
    }
}

@Composable
private fun AudioTaskMessageCard(
    message: String,
    isError: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}
