package com.yueliangmanle.danci.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSettingsScreen(
    state: AiSettingsUiState,
    onEnabledChange: (Boolean) -> Unit,
    onPlanAdjustmentsChange: (Boolean) -> Unit,
    onSessionCheckpointsChange: (Boolean) -> Unit,
    onDefaultProfileChange: (String?) -> Unit,
    onWordHelpProfileChange: (String?) -> Unit,
    onPlanAdjustmentProfileChange: (String?) -> Unit,
    onPhoneticFillProfileChange: (String?) -> Unit,
    onSelectProfile: (String) -> Unit,
    onNewProfileClick: () -> Unit,
    onEditorNameChange: (String) -> Unit,
    onEditorBaseUrlChange: (String) -> Unit,
    onEditorModelChange: (String) -> Unit,
    onEditorApiKeyChange: (String) -> Unit,
    onEditorEnabledChange: (Boolean) -> Unit,
    onSaveProfileClick: () -> Unit,
    onClearApiKeyClick: () -> Unit,
    onDeleteProfileClick: () -> Unit,
    onSaveGlobalClick: () -> Unit,
) {
    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text(
                text = "正在读取 AI 配置",
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
                                Color(0xFFF7E9D2),
                                Color(0xFFF4D9D0),
                            ),
                        ),
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "文文Lex AI API 中心",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "支持多 API 档案、本地加密存储、全局默认与功能级覆盖切换。音标补全、词条讲解和计划调整可以分开走不同接口。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "MiMo TTS 档案填写示例：Base URL = https://api.xiaomimimo.com/v1，模型 = mimo-v2-tts。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SettingSwitchRow(
            title = "AI 总开关",
            description = "关闭后不会发送任何学习数据，App 仍可完整离线使用。",
            checked = state.isEnabled,
            onCheckedChange = onEnabledChange,
        )
        SettingSwitchRow(
            title = "允许计划调整",
            description = "控制首页分析与学习中阶段建议是否启用。",
            checked = state.enablePlanAdjustments,
            onCheckedChange = onPlanAdjustmentsChange,
        )
        SettingSwitchRow(
            title = "允许会话检查点",
            description = "控制学习流中阶段分析建议是否启用。",
            checked = state.enableSessionCheckpoints,
            onCheckedChange = onSessionCheckpointsChange,
        )

        ProfileRoutingCard(
            title = "默认 API",
            description = "未单独指定时，所有 AI 功能都走这里。",
            selectedProfileId = state.defaultAiProfileId,
            profiles = state.profiles,
            allowFollowDefault = false,
            onSelect = onDefaultProfileChange,
        )
        ProfileRoutingCard(
            title = "词条讲解 API",
            description = "助记、近反义辨析、例句扩展等默认跟随全局，也可单独覆盖。",
            selectedProfileId = state.wordHelpProfileId,
            profiles = state.profiles,
            allowFollowDefault = true,
            onSelect = onWordHelpProfileChange,
        )
        ProfileRoutingCard(
            title = "计划调整 API",
            description = "首页策略分析与学习中阶段建议使用。",
            selectedProfileId = state.planAdjustmentProfileId,
            profiles = state.profiles,
            allowFollowDefault = true,
            onSelect = onPlanAdjustmentProfileChange,
        )
        ProfileRoutingCard(
            title = "音标补全 API",
            description = "单词详情和词书详情里的英式/美式音标补全使用。",
            selectedProfileId = state.phoneticFillProfileId,
            profiles = state.profiles,
            allowFollowDefault = true,
            onSelect = onPhoneticFillProfileChange,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "API 档案",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(onClick = onNewProfileClick) {
                        Text("新建档案")
                    }
                }
                if (state.profiles.isEmpty()) {
                    Text(
                        text = "还没有已保存的 API 档案，先新建一条即可。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    state.profiles.forEach { profile ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProfile(profile.id) },
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "${profile.providerType} · ${profile.model}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = profile.baseUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (profile.badges.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        profile.badges.forEach { badge ->
                                            FilterChip(
                                                selected = true,
                                                onClick = { onSelectProfile(profile.id) },
                                                label = { Text(badge) },
                                            )
                                        }
                                    }
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (state.editor.id == null) "新建档案" else "编辑档案",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = state.editor.name,
                    onValueChange = onEditorNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("档案名称") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.editor.baseUrl,
                    onValueChange = onEditorBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    supportingText = { Text("例如 https://api.openai.com/v1") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.editor.model,
                    onValueChange = onEditorModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模型") },
                    supportingText = { Text("例如 gpt-5-mini") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.editor.apiKeyInput,
                    onValueChange = onEditorApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = {
                        if (state.editor.hasSavedApiKey && state.editor.apiKeyInput.isBlank()) {
                            Text("已保存密钥，留空表示保持不变。")
                        } else {
                            Text("密钥会加密保存在本机，不会写入备份。")
                        }
                    },
                    singleLine = true,
                )
                SettingSwitchRow(
                    title = "启用这条档案",
                    description = "禁用后不会被默认路由或功能覆盖选中。",
                    checked = state.editor.enabled,
                    onCheckedChange = onEditorEnabledChange,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onSaveProfileClick,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving,
                    ) {
                        Text(if (state.isSaving) "保存中…" else "保存档案")
                    }
                    OutlinedButton(
                        onClick = onClearApiKeyClick,
                        modifier = Modifier.weight(1f),
                        enabled = state.editor.id != null && state.editor.hasSavedApiKey,
                    ) {
                        Text("清空密钥")
                    }
                }
                OutlinedButton(
                    onClick = onDeleteProfileClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.editor.id != null,
                ) {
                    Text("删除当前档案")
                }
            }
        }

        state.statusMessage?.let { message ->
            StatusCard(message = message, isError = false)
        }
        state.errorMessage?.let { message ->
            StatusCard(message = message, isError = true)
        }

        Button(
            onClick = onSaveGlobalClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
        ) {
            Text("保存全局路由与开关")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileRoutingCard(
    title: String,
    description: String,
    selectedProfileId: String?,
    profiles: List<AiProfileListItemUiState>,
    allowFollowDefault: Boolean,
    onSelect: (String?) -> Unit,
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
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (allowFollowDefault) {
                    FilterChip(
                        selected = selectedProfileId == null,
                        onClick = { onSelect(null) },
                        label = { Text("跟随默认") },
                    )
                }
                profiles.forEach { profile ->
                    FilterChip(
                        selected = selectedProfileId == profile.id,
                        onClick = { onSelect(profile.id) },
                        label = { Text(profile.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
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

@Composable
private fun SettingSwitchRow(
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
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
