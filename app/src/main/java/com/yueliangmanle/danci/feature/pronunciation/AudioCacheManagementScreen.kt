package com.yueliangmanle.danci.feature.pronunciation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AudioCacheManagementScreen(
    state: AudioCacheManagementUiState,
    onClearCacheClick: () -> Unit,
    onClearBucketClick: (String) -> Unit = {},
    onClearAllCachesClick: () -> Unit = {},
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
                Text("离线缓存管理", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "这里先管理词典音频缓存。后续本地 TTS 生成缓存和云端缓存也会统一收口到这里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "缓存上限 ${state.cacheLimitMb} MB",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    state.cacheSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = onClearCacheClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("清理词典缓存")
                }
                if (state.canClearAllCaches) {
                    OutlinedButton(
                        onClick = onClearAllCachesClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("清理全部本地缓存")
                    }
                }
            }
        }
        if (state.cacheBuckets.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "当前没有可管理的本地音频缓存桶。",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            state.cacheBuckets.forEach { bucket ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = bucket.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = bucket.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (bucket.canClear) {
                            OutlinedButton(
                                onClick = { onClearBucketClick(bucket.sourceType) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("清理这一类缓存")
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
private fun CacheMessageCard(
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
