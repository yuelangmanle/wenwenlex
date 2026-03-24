package com.yueliangmanle.danci.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yueliangmanle.danci.core.study.CardFeedback
import com.yueliangmanle.danci.core.study.StudyQueueEmptyState

@Composable
fun StudyScreen(
    state: StudyUiState,
    onFeedbackClick: (CardFeedback) -> Unit,
    onOpenDetailClick: () -> Unit,
    onBackHomeClick: () -> Unit,
    onPlayPronunciationClick: () -> Unit,
    onContinueNextGroupClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = state.sessionTitle,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = state.progressText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.isLoadingQueue) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "正在准备本轮单词…",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@Column
        }
        if (state.emptyState != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = emptyStateMessage(state.emptyState),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(onClick = onBackHomeClick) {
                        Text("返回首页")
                    }
                }
            }
            return@Column
        }
        if (state.groupSummaryTitle != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.groupSummaryTitle,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    state.groupSummaryBody?.let { body ->
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    if (state.showContinueNextGroup) {
                        OutlinedButton(onClick = onContinueNextGroupClick) {
                            Text("开始下一组")
                        }
                    }
                }
            }
            return@Column
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.passStepLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = state.currentWord,
                    style = MaterialTheme.typography.displaySmall,
                )
                state.phonetic?.let { phonetic ->
                    Text(
                        text = phonetic,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.meanings.isNotEmpty()) {
                    Text(
                        text = state.meanings.joinToString("；"),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                state.exampleSentence?.let { sentence ->
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                state.exampleTranslation?.let { translation ->
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.stepPrompt?.let { prompt ->
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onPlayPronunciationClick,
                ) {
                    Text("播放发音")
                }
                OutlinedButton(
                    onClick = onOpenDetailClick,
                ) {
                    Text("查看详情")
                }
            }
        }
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
        state.errorMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (state.checkpointTitle != null && state.checkpointSuggestion != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.checkpointTitle,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.checkpointSourceLabel?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = state.checkpointSuggestion,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (state.isSessionComplete) {
            Button(
                onClick = onOpenDetailClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("返回单词详情")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onFeedbackClick(CardFeedback.NOT_KNOWN) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("不认识")
                }
                Button(
                    onClick = { onFeedbackClick(CardFeedback.FUZZY) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("模糊")
                }
                Button(
                    onClick = { onFeedbackClick(CardFeedback.KNOWN) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("认识")
                }
            }
        }
    }
}

private fun emptyStateMessage(emptyState: StudyQueueEmptyState): String =
    when (emptyState) {
        StudyQueueEmptyState.NO_NEW_WORDS -> "今天的新词已完成"
        StudyQueueEmptyState.NO_DUE_REVIEW -> "当前没有到期复习词"
        StudyQueueEmptyState.NO_RECENT_MISTAKES -> "最近没有需要回拉的错词"
    }
