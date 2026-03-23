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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PronunciationSourceDetailScreen(
    state: PronunciationSourceDetailUiState,
    onBindProfileClick: (String) -> Unit,
    onSelectPresetClick: (String) -> Unit,
    onAdvancedStyleChange: (String) -> Unit,
    onTestTextChange: (String) -> Unit,
    onGenerationWordTextChange: (String) -> Unit,
    onGenerationBatchSizeTextChange: (String) -> Unit,
    onCheckApiClick: () -> Unit,
    onGenerateSingleWordClick: () -> Unit,
    onGenerateBookClick: (String) -> Unit,
    onGenerateBatchClick: (String) -> Unit,
    onOpenTaskCenterClick: () -> Unit,
) {
    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text(
                text = "正在读取发音源详情",
                modifier = Modifier.padding(top = 12.dp),
            )
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
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = state.sourceTitle,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = state.sourceSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "口音：${state.accentLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (state.isCloudSource) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "云端 TTS 档案",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (state.profileOptions.isEmpty()) {
                        Text(
                            text = "当前还没有可用的 MiMo 档案。先去 AI API 中心新增一条 `https://api.xiaomimimo.com/v1 + mimo-v2-tts` 的档案。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        state.profileOptions.forEach { option ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(option.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        option.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    OutlinedButton(
                                        onClick = { onBindProfileClick(option.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = state.selectedProfileId != option.id,
                                    ) {
                                        Text(if (state.selectedProfileId == option.id) "当前已绑定" else "绑定这条档案")
                                    }
                                }
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
                    Text(
                        text = "MiMo 预设",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.presetOptions.forEach { preset ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                if (preset.summary.isNotBlank()) {
                                    Text(
                                        text = preset.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onSelectPresetClick(preset.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !preset.isDefault,
                                ) {
                                    Text(if (preset.isDefault) "当前预设" else "切换为默认预设")
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.advancedStyleText,
                        onValueChange = onAdvancedStyleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("高级 Style 文本") },
                        supportingText = {
                            Text("仅在你需要覆盖预设时填写，会直接注入到 `<style>...</style>`。")
                        },
                    )
                    OutlinedTextField(
                        value = state.testText,
                        onValueChange = onTestTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("检测文本") },
                        supportingText = { Text("用于 API 检测和返回音频校验。") },
                    )
                    Button(
                        onClick = onCheckApiClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canCheckApi,
                    ) {
                        Text("检测 API")
                    }
                    state.healthCheckSummary?.let { summary ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("检测结果", style = MaterialTheme.typography.titleSmall)
                                Text(summary, style = MaterialTheme.typography.bodyMedium)
                                state.healthCheckLatencyLabel?.let { latency ->
                                    Text(
                                        latency,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.isLocalSource) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "本地发音源",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "这个来源由本地语音包驱动。下载、安装和删除仍然在发音中心主页里操作；这里主要展示来源状态，后面会继续补生成入口。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.isCloudSource || state.isLocalSource) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "缓存生成",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = state.generationSupportMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.generationSupported) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    OutlinedTextField(
                        value = state.generationWordText,
                        onValueChange = onGenerationWordTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("单词（必须已经在词库里）") },
                    )
                    OutlinedTextField(
                        value = state.generationBatchSizeText,
                        onValueChange = onGenerationBatchSizeTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("分批大小") },
                        supportingText = { Text("例如 50，表示每次先生成 50 个词。") },
                    )
                    Button(
                        onClick = onGenerateSingleWordClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.generationSupported,
                    ) {
                        Text("生成当前单词缓存")
                    }
                    if (state.bookOptions.isEmpty()) {
                        Text(
                            text = "当前还没有可生成的词书。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.bookOptions.forEach { book ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        book.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Button(
                                        onClick = { onGenerateBookClick(book.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = state.generationSupported,
                                    ) {
                                        Text("整本后台生成")
                                    }
                                    OutlinedButton(
                                        onClick = { onGenerateBatchClick(book.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = state.generationSupported,
                                    ) {
                                        Text("分批生成")
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenTaskCenterClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("打开任务中心")
                    }
                }
            }
        }

        state.statusMessage?.let { message ->
            MessageCard(message = message, isError = false)
        }
        state.errorMessage?.let { message ->
            MessageCard(message = message, isError = true)
        }
    }
}
