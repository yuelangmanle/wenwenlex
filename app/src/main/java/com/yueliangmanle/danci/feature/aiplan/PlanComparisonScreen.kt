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
import com.yueliangmanle.danci.core.model.PlanHistoryEntry

@Composable
fun PlanComparisonScreen(
    state: PlanComparisonUiState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TitleCard(
            title = "计划对比",
            summary = "左侧固定是当前生效方案，右侧是你选中的历史版本。",
        )
        ComparisonPlanCard(
            title = "当前计划",
            plan = state.currentPlan,
        )
        ComparisonPlanCard(
            title = "历史版本",
            plan = state.targetPlan,
        )
        DiffCard(
            title = "重点差异",
            lines = state.focusDiff,
        )
        state.paceDiff?.let {
            SingleLineCard(
                title = "节奏差异",
                value = it,
            )
        }
        if (state.modeDiff.isNotEmpty()) {
            DiffCard(
                title = "模式差异",
                lines = state.modeDiff,
            )
        }
        state.executionEffectDiff?.let {
            SingleLineCard(
                title = "预期影响",
                value = it,
            )
        }
        state.errorMessage?.let {
            SingleLineCard(
                title = "提示",
                value = it,
            )
        }
    }
}

@Composable
private fun TitleCard(
    title: String,
    summary: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComparisonPlanCard(
    title: String,
    plan: PlanHistoryEntry?,
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
            if (plan == null) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = plan.summary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "重点：${plan.recommendedFocus.joinToString("、").ifBlank { "未设置" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "模式：${plan.suggestedModes.joinToString("、").ifBlank { "未设置" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DiffCard(
    title: String,
    lines: List<String>,
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
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SingleLineCard(
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
