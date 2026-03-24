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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationMode

@Composable
fun PronunciationSettingsScreen(
    state: PronunciationSettingsUiState,
    onRefreshCatalog: () -> Unit,
    onSelectAccent: (String) -> Unit,
    onSelectMode: (String) -> Unit,
    onAutoCacheChanged: (Boolean) -> Unit,
    onAllowCellularChanged: (Boolean) -> Unit,
    onFallbackToSystemTtsChanged: (Boolean) -> Unit,
    onPreferOfflineLongTextChanged: (Boolean) -> Unit,
    onClearCacheClick: () -> Unit,
    onOpenAudioCacheManagementClick: () -> Unit,
    onActivateVoicePack: (String) -> Unit,
    onDownloadVoicePack: (String) -> Unit,
    onRemoveVoicePack: (String) -> Unit,
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
                Text("发音与朗读", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "单词优先走词典音频，例句和长文本后续可接离线语音包；兜底仍由系统朗读承担。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onRefreshCatalog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("刷新语音包清单")
                }
                if (state.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text("正在处理发音设置…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        OptionCard(
            title = "默认口音",
            description = "详情页和学习页默认优先播放的口音。",
        ) {
            AccentButtons(
                selectedAccent = state.preferredAccent,
                onSelectAccent = onSelectAccent,
            )
        }
        OptionCard(
            title = "发音来源策略",
            description = "当前推荐保持词典音频优先。",
        ) {
            ModeButtons(
                selectedMode = state.pronunciationMode,
                onSelectMode = onSelectMode,
            )
        }
        ToggleCard(
            title = "自动缓存单词音频",
            description = state.audioCacheSummary,
            checked = state.autoCacheWordAudio,
            onCheckedChange = onAutoCacheChanged,
        )
        ToggleCard(
            title = "语音包允许移动网络下载",
            description = "关闭后仅在 Wi-Fi 下允许下载离线语音包。",
            checked = state.allowCellularVoicePackDownload,
            onCheckedChange = onAllowCellularChanged,
        )
        ToggleCard(
            title = "系统朗读兜底",
            description = "找不到词典音频时是否允许使用系统朗读。",
            checked = state.fallbackToSystemTts,
            onCheckedChange = onFallbackToSystemTtsChanged,
        )
        ToggleCard(
            title = "长文本优先离线朗读",
            description = "语音包接通后会优先用于例句和 AI 讲解朗读。",
            checked = state.preferOfflineForLongText,
            onCheckedChange = onPreferOfflineLongTextChanged,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("音频缓存", style = MaterialTheme.typography.titleMedium)
                Text(
                    "上限 ${state.audioCacheLimitMb} MB · ${state.audioCacheSummary}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onOpenAudioCacheManagementClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("进入缓存管理")
                }
                OutlinedButton(
                    onClick = onClearCacheClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("快速清空词典缓存")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("离线语音包", style = MaterialTheme.typography.titleMedium)
                Text(
                    "支持桥接包和原生离线发音包的一键下载、安装、激活和删除；原生包会额外展示导入词书支持和资源占用提示。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.voicePacks.isEmpty()) {
                    Text(
                        "当前还没有可用语音包清单。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    state.voicePacks.forEach { pack ->
                        VoicePackRow(
                            pack = pack,
                            onActivate = { onActivateVoicePack(pack.id) },
                            onDownload = { onDownloadVoicePack(pack.id) },
                            onRemove = { onRemoveVoicePack(pack.id) },
                        )
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

@Composable
private fun OptionCard(
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
private fun ToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun AccentButtons(
    selectedAccent: String,
    onSelectAccent: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PronunciationAccent.entries.filter { it != PronunciationAccent.AUTO }.forEach { accent ->
            val selected = accent.storageValue == selectedAccent
            if (selected) {
                Button(
                    onClick = { onSelectAccent(accent.storageValue) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(accent.label)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelectAccent(accent.storageValue) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(accent.label)
                }
            }
        }
    }
}

@Composable
private fun ModeButtons(
    selectedMode: String,
    onSelectMode: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PronunciationMode.entries.forEach { mode ->
            val selected = mode.storageValue == selectedMode
            val label = if (mode == PronunciationMode.DICTIONARY_FIRST) "词典优先" else "离线优先"
            if (selected) {
                Button(
                    onClick = { onSelectMode(mode.storageValue) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelectMode(mode.storageValue) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun VoicePackRow(
    pack: VoicePackItemUiState,
    onActivate: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(pack.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${pack.locale} · ${pack.versionLabel} · ${pack.engineLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                pack.capabilitySummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                pack.resourceHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (pack.isActive) "${pack.statusLabel} · 当前已启用" else pack.statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (pack.failureReason != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            pack.failureReason?.let { reason ->
                Text(
                    reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDownload,
                    enabled = pack.canDownload,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (pack.isBusy) "处理中…" else if (pack.canDownload) "下载/安装" else "已安装")
                }
                OutlinedButton(
                    onClick = onActivate,
                    enabled = pack.canActivate,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (pack.isActive) "当前已启用" else "设为默认")
                }
            }
            if (pack.canDelete) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除语音包")
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
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
