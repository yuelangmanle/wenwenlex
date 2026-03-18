package com.yueliangmanle.danci.feature.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun QuizScreen(
    state: QuizUiState,
    onOptionClick: (String) -> Unit,
    onOpenDetailClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "选择题复习",
            style = MaterialTheme.typography.titleLarge,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = state.prompt,
                    style = MaterialTheme.typography.headlineMedium,
                )
                state.options.forEach { option ->
                    Button(
                        onClick = { onOptionClick(option) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(option)
                    }
                }
            }
        }
        state.explanation?.let { explanation ->
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = explanation,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (state.aiReviewTitle != null && state.aiReviewBody != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.aiReviewTitle,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.aiReviewSourceLabel?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = state.aiReviewBody,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        OutlinedButton(
            onClick = onOpenDetailClick,
        ) {
            Text("返回单词详情")
        }
    }
}
