package com.yueliangmanle.danci.feature.books

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BookImportScreen(
    state: BookImportUiState,
    onBookTitleChange: (String) -> Unit,
    onPickFileClick: () -> Unit,
    onAiNormalizeClick: () -> Unit,
    onImportClick: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "导入 Excel 词书",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "格式默认按 A 列单词、B 列中文义解析；中英文逗号都能拆分。原表不规整时，可以直接点“AI 适配表格”。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onPickFileClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.fileName == null) "选择 Excel 文件" else "重新选择文件")
                    }
                    OutlinedButton(
                        onClick = onAiNormalizeClick,
                        modifier = Modifier.weight(1f),
                        enabled = state.preview != null && !state.isAiNormalizing,
                    ) {
                        Text(if (state.isAiNormalizing) "AI 适配中…" else "AI 适配表格")
                    }
                }
            }
        }

        if (state.fileName != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "当前文件：${state.fileName}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = state.bookTitle,
                        onValueChange = onBookTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("词书名称") },
                        supportingText = { Text("导入后会作为新词书显示在“导入词书”分组里。") },
                        singleLine = true,
                    )
                }
            }
        }

        state.statusMessage?.let { message ->
            StatusCard(message = message, isError = false)
        }
        state.errorMessage?.let { message ->
            StatusCard(message = message, isError = true)
        }

        state.preview?.let { preview ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "解析预览",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "工作表：${preview.sheetName} · 识别 ${preview.rows.size} 行 · 跳过 ${preview.skippedRows} 行 · 模式 ${preview.parserMode}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    preview.warningMessage?.let { warning ->
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    defaultPreviewRows(preview).forEach { row ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = row.word,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = row.meanings.joinToString("；"),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                val phoneticLabel = listOfNotNull(
                                    row.phoneticUk?.takeIf(String::isNotBlank)?.let { "英：$it" },
                                    row.phoneticUs?.takeIf(String::isNotBlank)?.let { "美：$it" },
                                ).joinToString("  ")
                                if (phoneticLabel.isNotBlank()) {
                                    Text(
                                        text = phoneticLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = row.sourceLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    if (preview.rows.size > 20) {
                        Text(
                            text = "仅预览前 20 行，导入时会处理全部识别出的词条。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onImportClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = preview.rows.isNotEmpty() && !state.isImporting,
                    ) {
                        Text(if (state.isImporting) "导入中…" else "确认导入")
                    }
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
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}
