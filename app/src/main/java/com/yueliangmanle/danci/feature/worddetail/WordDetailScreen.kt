package com.yueliangmanle.danci.feature.worddetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WordDetailScreen(
    state: WordDetailUiState,
    onPlayUkPronunciationClick: () -> Unit = {},
    onPlayUsPronunciationClick: () -> Unit = {},
    onAiMemoryClick: () -> Unit = {},
    onAiContrastClick: () -> Unit = {},
    onExplainWordFormsClick: () -> Unit = {},
    onExpandExampleClick: () -> Unit = {},
    onFillMissingPhoneticClick: () -> Unit = {},
    onOverwritePhoneticClick: () -> Unit = {},
    onSwitchPronunciationSource: (String?) -> Unit = {},
    onStartQuizClick: () -> Unit = {},
) {
    var pronunciationSourceExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = state.word,
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = state.phoneticStatusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = state.phoneticSourceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "英式：${state.phoneticUk ?: "待补全"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "美式：${state.phoneticUs ?: state.phonetic ?: "待补全"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onPlayUkPronunciationClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("播放英式")
                    }
                    OutlinedButton(
                        onClick = onPlayUsPronunciationClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("播放美式")
                    }
                }
                Text(
                    text = "当前发音源：${state.selectedPronunciationSourceLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box {
                    OutlinedButton(
                        onClick = { pronunciationSourceExpanded = true },
                    ) {
                        Text("切换发音源")
                    }
                    DropdownMenu(
                        expanded = pronunciationSourceExpanded,
                        onDismissRequest = { pronunciationSourceExpanded = false },
                    ) {
                        state.availablePronunciationSources.forEach { source ->
                            DropdownMenuItem(
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(source.title)
                                        Text(
                                            text = source.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = {
                                    pronunciationSourceExpanded = false
                                    onSwitchPronunciationSource(source.id)
                                },
                            )
                        }
                    }
                }
                if (state.meanings.isNotEmpty()) {
                    Text(
                        text = state.meanings.joinToString("；"),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                state.exampleSentence?.let { sentence ->
                    Text(sentence, style = MaterialTheme.typography.bodyMedium)
                }
                state.exampleTranslation?.let { translation ->
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onFillMissingPhoneticClick,
                modifier = Modifier.weight(1f),
                enabled = !state.isPhoneticLoading,
            ) {
                Text(if (state.isPhoneticLoading) "处理中…" else "补空白音标")
            }
            OutlinedButton(
                onClick = onOverwritePhoneticClick,
                modifier = Modifier.weight(1f),
                enabled = !state.isPhoneticLoading,
            ) {
                Text("覆盖重拉")
            }
        }

        state.statusMessage?.let { message ->
            MessageCard(message = message, isError = false)
        }
        state.errorMessage?.let { message ->
            MessageCard(message = message, isError = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onAiMemoryClick,
                modifier = Modifier.weight(1f),
            ) {
                Text("AI 助记")
            }
            OutlinedButton(
                onClick = onAiContrastClick,
                modifier = Modifier.weight(1f),
            ) {
                Text("近反义辨析")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onExplainWordFormsClick,
                modifier = Modifier.weight(1f),
            ) {
                Text("词形讲解")
            }
            OutlinedButton(
                onClick = onExpandExampleClick,
                modifier = Modifier.weight(1f),
            ) {
                Text("例句扩展")
            }
        }
        if (state.isAiLoading) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AI 正在整理这条提示…",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        state.aiCards.forEach { card ->
            AiInsightCard(card = card)
        }
        DetailSection(title = "近义词", items = state.synonyms)
        DetailSection(title = "反义词", items = state.antonyms)
        DetailSection(title = "拼写相近词", items = state.similarWords)
        DetailSection(title = "易混词", items = state.confusingWords.ifEmpty { state.similarWords })
        DetailSection(title = "单词变形", items = state.wordForms)
        DetailSection(title = "词根词缀", items = listOfNotNull(state.root))
        OutlinedButton(
            onClick = onStartQuizClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("开始选择题复习")
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

@Composable
private fun AiInsightCard(card: AiInsightCardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
            )
            card.sourceLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = card.body,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (card.bullets.isNotEmpty()) {
                Text(
                    text = card.bullets.joinToString("\n") { "• $it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    items: List<String>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (items.isEmpty()) "暂无" else items.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
