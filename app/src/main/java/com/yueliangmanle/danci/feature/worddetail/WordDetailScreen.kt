package com.yueliangmanle.danci.feature.worddetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WordDetailScreen(
    state: WordDetailUiState,
    onAiMemoryClick: () -> Unit,
    onAiContrastClick: () -> Unit,
    onStartQuizClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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

        DetailSection(title = "近义词", items = state.synonyms)
        DetailSection(title = "反义词", items = state.antonyms)
        DetailSection(title = "拼写相近词", items = state.similarWords)
        DetailSection(title = "易混词", items = state.confusingWords.ifEmpty { state.similarWords })
        DetailSection(title = "单词变形", items = state.wordForms)
        DetailSection(title = "词根词缀", items = listOfNotNull(state.root))

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
        OutlinedButton(
            onClick = onStartQuizClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("开始选择题复习")
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
