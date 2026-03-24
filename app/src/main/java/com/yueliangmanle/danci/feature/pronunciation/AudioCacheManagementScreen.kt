package com.yueliangmanle.danci.feature.pronunciation

import androidx.compose.foundation.horizontalScroll
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
fun AudioCacheManagementScreen(
    state: AudioCacheManagementUiState,
    onSelectSource: (String) -> Unit,
    onSelectBook: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onDecreaseLimit: () -> Unit,
    onIncreaseLimit: () -> Unit,
    onTrimToLimit: () -> Unit,
    onClearBucket: (String) -> Unit,
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
                Text("音频缓存管理", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "集中查看各类词频音频缓存、手动清理单个来源，并按上限整理磁盘占用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "当前缓存 ${state.totalSummary} · 上限 ${state.cacheLimitMb} MB",
                    style = MaterialTheme.typography.bodyLarge,
                )
                state.overLimitSummary?.let { summary ->
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDecreaseLimit,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("-50 MB")
                    }
                    OutlinedButton(
                        onClick = onIncreaseLimit,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("+50 MB")
                    }
                }
                OutlinedButton(
                    onClick = onTrimToLimit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("按上限整理缓存")
                }
                if (state.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text("正在整理缓存数据…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("筛选", style = MaterialTheme.typography.titleMedium)
                Text(
                    "按来源、词书和关键词筛选缓存条目。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilterRow {
                    state.buckets.forEach { bucket ->
                        SelectableActionButton(
                            selected = bucket.isSelected,
                            label = "${bucket.label} (${bucket.count})",
                            onClick = { onSelectSource(bucket.sourceType) },
                        )
                    }
                }
                FilterRow {
                    state.books.forEach { book ->
                        SelectableActionButton(
                            selected = book.isSelected,
                            label = book.title,
                            onClick = { onSelectBook(book.id) },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("关键词") },
                    placeholder = { Text("输入单词或中文释义") },
                    singleLine = true,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("来源分桶", style = MaterialTheme.typography.titleMedium)
                if (state.buckets.isEmpty()) {
                    Text("当前没有可管理的缓存音频。", style = MaterialTheme.typography.bodyMedium)
                } else {
                    state.buckets.forEach { bucket ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(bucket.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${bucket.count} 条 · ${bucket.sizeSummary}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(
                                    onClick = { onClearBucket(bucket.sourceType) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("清理这一桶")
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
                Text("缓存条目", style = MaterialTheme.typography.titleMedium)
                if (state.items.isEmpty()) {
                    Text("当前筛选条件下没有缓存条目。", style = MaterialTheme.typography.bodyMedium)
                } else {
                    state.items.forEach { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(item.word, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    item.meaningsSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${item.sourceLabel} · ${item.accentLabel} · ${item.sizeSummary}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        state.statusMessage?.let { message ->
            CacheMessageCard(message = message, isError = false)
        }
        state.errorMessage?.let { message ->
            CacheMessageCard(message = message, isError = true)
        }
    }
}

@Composable
private fun FilterRow(
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun SelectableActionButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label)
        }
    }
}

@Composable
private fun CacheMessageCard(
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
