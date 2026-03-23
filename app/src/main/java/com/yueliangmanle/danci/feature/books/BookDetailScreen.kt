package com.yueliangmanle.danci.feature.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun BookDetailScreen(
    state: BookDetailUiState,
    onSetActiveBookClick: () -> Unit,
    onFillMissingPhoneticsClick: () -> Unit,
    onOverwritePhoneticsClick: () -> Unit,
    onStartQualityEnrichmentClick: () -> Unit,
    onWordClick: (Long) -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (state.description.isNotBlank()) {
                        Text(
                            text = state.description,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "${state.sourceLabel} · ${state.wordCount} 词",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    state.sourceMeta?.let { meta ->
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = state.phoneticCoverage,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    state.latestImportSummary?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onSetActiveBookClick,
                            modifier = Modifier.weight(1f),
                            enabled = !state.isActiveBook,
                        ) {
                            Text(if (state.isActiveBook) "已是当前词书" else "设为当前词书")
                        }
                        OutlinedButton(
                            onClick = onFillMissingPhoneticsClick,
                            modifier = Modifier.weight(1f),
                            enabled = !state.isFilling,
                        ) {
                            Text(if (state.isFilling) "处理中…" else "只补空白")
                        }
                    }
                    OutlinedButton(
                        onClick = onOverwritePhoneticsClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isFilling,
                    ) {
                        Text("覆盖全部音标")
                    }
                    if (state.canStartQualityEnrichment) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "质量补强",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                state.qualityEnrichmentSummary?.let { summary ->
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                OutlinedButton(
                                    onClick = onStartQualityEnrichmentClick,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("开始质量补强")
                                }
                            }
                        }
                    }
                }
            }
        }
        state.statusMessage?.let { message ->
            item {
                MessageCard(message = message, isError = false)
            }
        }
        state.errorMessage?.let { message ->
            item {
                MessageCard(message = message, isError = true)
            }
        }
        items(state.words, key = BookWordItemUiState::id) { word ->
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clickable { onWordClick(word.id) },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = word.word,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = word.phoneticStatusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = word.phoneticLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = word.meaningsLabel,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}
