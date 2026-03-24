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
fun CloudTtsSettingsScreen(
    state: CloudTtsSettingsUiState,
    onSelectProvider: (String) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSaveEndpointClick: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSaveApiKeyClick: () -> Unit,
    onClearApiKeyClick: () -> Unit,
    onCheckApiClick: () -> Unit,
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
                Text("云端 TTS", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "当前先接入 MiMo，配置完成后可以作为词典失败后的云端发音兜底，并把生成结果写入本地缓存桶。",
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
                        Text("正在加载云端 TTS 设置…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        CloudTtsOptionCard(
            title = "服务提供方",
            description = "后续可扩展多个云端 TTS provider 并快速切换。",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.providers.forEach { provider ->
                    if (provider.isSelected) {
                        Button(
                            onClick = { onSelectProvider(provider.id) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(provider.label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectProvider(provider.id) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(provider.label)
                        }
                    }
                }
            }
        }
        CloudTtsOptionCard(
            title = "声线预设",
            description = "默认预设会用于云端实时生成和后续批量缓存任务。",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.presets.forEach { preset ->
                    if (preset.isSelected) {
                        Button(
                            onClick = { onSelectPreset(preset.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${preset.label} · ${preset.voice}")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectPreset(preset.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${preset.label} · ${preset.voice}")
                        }
                    }
                }
            }
        }
        CloudTtsOptionCard(
            title = "接口配置",
            description = "MiMo 文档当前采用 OpenAI 兼容的 `/chat/completions` 语音合成接口。",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = onBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.model,
                    onValueChange = onModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模型") },
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = onSaveEndpointClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("保存接口配置")
                }
            }
        }
        CloudTtsOptionCard(
            title = "API Key",
            description = if (state.hasSavedApiKey) {
                "当前 provider 已保存 API Key，保存在本机安全存储。"
            } else {
                "当前 provider 还没有保存 API Key。"
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.apiKeyInput,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onSaveApiKeyClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("保存密钥")
                    }
                    OutlinedButton(
                        onClick = onClearApiKeyClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("清除密钥")
                    }
                }
                OutlinedButton(
                    onClick = onCheckApiClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("检测 API")
                }
            }
        }
        state.statusMessage?.let { message ->
            CloudTtsMessageCard(message = message, isError = false)
        }
        state.errorMessage?.let { message ->
            CloudTtsMessageCard(message = message, isError = true)
        }
    }
}

@Composable
private fun CloudTtsOptionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun CloudTtsMessageCard(
    message: String,
    isError: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}
