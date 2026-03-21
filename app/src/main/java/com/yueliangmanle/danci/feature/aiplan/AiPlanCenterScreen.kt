package com.yueliangmanle.danci.feature.aiplan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AiPlanCenterScreen(
    state: AiPlanCenterUiState,
    onConfirmPendingPlanClick: (Long) -> Unit,
    onRejectPendingPlanClick: (Long) -> Unit,
    onOpenComparisonClick: (Long) -> Unit,
    onOpenExplanationClick: (Long) -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text("正在整理 AI 计划历史")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard()
        state.currentPlanSummary?.let { summary ->
            CurrentPlanCard(
                summary = summary,
                meta = state.currentPlanMeta,
            )
        }
        state.pendingPlan?.let { pendingPlan ->
            PendingPlanCard(
                plan = pendingPlan,
                onConfirmPendingPlanClick = onConfirmPendingPlanClick,
                onRejectPendingPlanClick = onRejectPendingPlanClick,
            )
        }
        if (state.timeline.isEmpty() && state.pendingPlan == null && state.currentPlanSummary == null) {
            EmptyStateCard(message = state.emptyMessage ?: AI_PLAN_CENTER_EMPTY_MESSAGE)
        } else {
            TimelineCard(
                timeline = state.timeline,
            )
        }
        state.statusMessage?.let { message ->
            MessageCard(
                message = message,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        state.errorMessage?.let { message ->
            MessageCard(
                message = message,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF8F5EE),
                            Color(0xFFE6F4EA),
                            Color(0xFFD9EAFD),
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "AI 计划中心",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "把每次学习调整的原因、决策和历史版本集中在一起看清楚。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CurrentPlanCard(
    summary: String,
    meta: String?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "当前生效计划",
                style = MaterialTheme.typography.titleMedium,
            )
            meta?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PendingPlanCard(
    plan: PlanHistoryEntry,
    onConfirmPendingPlanClick: (Long) -> Unit,
    onRejectPendingPlanClick: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "待确认调整",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "这次改动幅度较大，建议先看原因，再决定是否生效。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DecisionChip(plan = plan)
            Text(
                text = plan.summary,
                style = MaterialTheme.typography.bodyLarge,
            )
            plan.reasonSummary?.let {
                LabelValueRow(
                    label = "原因",
                    value = it,
                )
            }
            plan.changeSummary?.let {
                LabelValueRow(
                    label = "变化",
                    value = it,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onConfirmPendingPlanClick(plan.id) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("确认应用")
                }
                OutlinedButton(
                    onClick = { onRejectPendingPlanClick(plan.id) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("暂不采用")
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(
    timeline: List<PlanHistoryEntry>,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "调整时间轴",
                style = MaterialTheme.typography.titleMedium,
            )
            if (timeline.isEmpty()) {
                Text(
                    text = "当前还没有可以回看的历史版本。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                timeline.forEach { entry ->
                    TimelineItem(
                        entry = entry,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    entry: PlanHistoryEntry,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlanTimestamp(entry),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DecisionChip(plan = entry)
        }
        Text(
            text = entry.summary,
            style = MaterialTheme.typography.bodyLarge,
        )
        entry.changeSummary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DecisionChip(plan: PlanHistoryEntry) {
    val color = when (plan.applyStatus) {
        PlanApplyStatus.APPLIED -> MaterialTheme.colorScheme.primary
        PlanApplyStatus.PENDING_CONFIRMATION -> MaterialTheme.colorScheme.error
        PlanApplyStatus.REJECTED -> MaterialTheme.colorScheme.outline
        PlanApplyStatus.SUPERSEDED -> MaterialTheme.colorScheme.secondary
    }
    Text(
        text = when (plan.applyStatus) {
            PlanApplyStatus.APPLIED -> "已生效"
            PlanApplyStatus.PENDING_CONFIRMATION -> "待确认"
            PlanApplyStatus.REJECTED -> "已拒绝"
            PlanApplyStatus.SUPERSEDED -> "已覆盖"
        },
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

@Composable
private fun LabelValueRow(
    label: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageCard(
    message: String,
    color: Color,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

private fun formatPlanTimestamp(entry: PlanHistoryEntry): String =
    entry.generatedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
