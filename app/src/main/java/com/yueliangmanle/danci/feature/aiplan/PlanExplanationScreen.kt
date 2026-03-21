package com.yueliangmanle.danci.feature.aiplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlanExplanationScreen(
    state: PlanExplanationUiState,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "计划解释",
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (state.headline.isNotBlank()) {
                    Text(
                        text = state.headline,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                val meta = listOf(state.sourceLabel, state.decisionLabel).filter(String::isNotBlank)
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        ExplanationSection(
            title = "为什么改",
            value = state.reasonSummary.ifBlank { "当前没有额外原因说明。" },
        )
        ExplanationSection(
            title = "改了什么",
            value = state.changeSummary.ifBlank { "当前没有可展示的改动摘要。" },
        )
        state.executionEffect?.let {
            ExplanationSection(
                title = "预期影响",
                value = it,
            )
        }
        if (state.abnormalSignals.isNotEmpty()) {
            ExplanationSection(
                title = "触发信号",
                value = state.abnormalSignals.joinToString("、"),
            )
        }
        state.errorMessage?.let {
            ExplanationSection(
                title = "提示",
                value = it,
            )
        }
    }
}

@Composable
private fun ExplanationSection(
    title: String,
    value: String,
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
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
