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

@Composable
fun StudyScreen(
    state: StudyUiState,
    onFeedbackClick: (CardFeedback) -> Unit,
    onSkipClick: () -> Unit,
    onOpenDetailClick: () -> Unit,
    onPlayPronunciationClick: () -> Unit,
    onOpenPlanCenterClick: () -> Unit,
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
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                    state.checkpointDecisionLabel?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (label == "需要确认") {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    Text(
                        text = state.checkpointSuggestion,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.canOpenPlanCenter) {
                        OutlinedButton(
                            onClick = onOpenPlanCenterClick,
                        ) {
                            Text("查看 AI 计划中心")
                        }
                    }
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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                OutlinedButton(
                    onClick = onSkipClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("暂时跳过")
                }
            }
        }
    }
}
