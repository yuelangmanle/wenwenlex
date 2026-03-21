package com.yueliangmanle.danci.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartNewWordsClick: () -> Unit,
    onStartReviewClick: () -> Unit,
    onOpenMistakesClick: () -> Unit,
    onAnalyzePlanClick: () -> Unit,
    onOpenLearningAnalyticsClick: () -> Unit,
    onOpenPlanCenterClick: () -> Unit,
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
                Text(
                    text = "正在生成今天的学习计划",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(state = state)
        }
        state.errorMessage?.let { message ->
            item {
                StatusCard(
                    message = message,
                    isError = true,
                )
            }
        }
        item {
            StatsGrid(state = state)
        }
        item {
            ActionPanel(
                state = state,
                onStartNewWordsClick = onStartNewWordsClick,
                onStartReviewClick = onStartReviewClick,
                onOpenMistakesClick = onOpenMistakesClick,
                onAnalyzePlanClick = onAnalyzePlanClick,
                onOpenLearningAnalyticsClick = onOpenLearningAnalyticsClick,
            )
        }
        if (state.planCenterTitle != null && state.planCenterSummary != null) {
            item {
                PlanCenterCard(
                    title = state.planCenterTitle,
                    summary = state.planCenterSummary,
                    pendingPlanCount = state.pendingPlanCount,
                    meta = state.planCenterMeta,
                    onOpenPlanCenterClick = onOpenPlanCenterClick,
                )
            }
        }
        state.aiSuggestion?.let { suggestion ->
            item {
                AiHintCard(
                    suggestion = suggestion,
                    title = state.aiSuggestionTitle,
                    meta = state.aiSuggestionMeta,
                    focusWords = state.aiFocusWords,
                )
            }
        }
    }
}

@Composable
private fun PlanCenterCard(
    title: String,
    summary: String,
    pendingPlanCount: Int,
    meta: String? = null,
    onOpenPlanCenterClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (pendingPlanCount > 0) {
                Text(
                    text = "有 $pendingPlanCount 条待确认调整",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
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
            OutlinedButton(
                onClick = onOpenPlanCenterClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("查看 AI 计划中心")
            }
        }
    }
}

@Composable
private fun StatusCard(
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
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun HeroCard(state: HomeUiState) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFCE7C8),
                            Color(0xFFF7C9B6),
                            Color(0xFFE8EEF9),
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = state.activeBookTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.headline,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "今日目标 ${state.todayGoalCount} 词 · 预计 ${state.estimatedMinutes} 分钟完成",
                style = MaterialTheme.typography.bodyLarge,
            )
            LinearProgressIndicator(
                progress = { calculateGoalProgress(state) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
            )
        }
    }
}

@Composable
private fun StatsGrid(state: HomeUiState) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "新词",
                value = state.newWordCount.toString(),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "复习",
                value = state.reviewCount.toString(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "错词回顾",
                value = state.mistakeCount.toString(),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "连续学习",
                value = "${state.streakDays} 天",
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun ActionPanel(
    state: HomeUiState,
    onStartNewWordsClick: () -> Unit,
    onStartReviewClick: () -> Unit,
    onOpenMistakesClick: () -> Unit,
    onAnalyzePlanClick: () -> Unit,
    onOpenLearningAnalyticsClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "今天先做什么",
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = onStartNewWordsClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("开始新词学习")
            }
            Button(
                onClick = onStartReviewClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("开始复习")
            }
            OutlinedButton(
                onClick = onOpenLearningAnalyticsClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("查看学习统计")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenMistakesClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("最近错词")
                }
                OutlinedButton(
                    onClick = onAnalyzePlanClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isAnalyzingPlan) "分析中…" else "分析并调整计划")
                }
            }
        }
    }
}

@Composable
private fun AiHintCard(
    suggestion: String,
    title: String? = null,
    meta: String? = null,
    focusWords: List<String> = emptyList(),
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title ?: "AI 计划提示",
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
                text = suggestion,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (focusWords.isNotEmpty()) {
                Text(
                    text = "重点词群：${focusWords.joinToString(" · ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun calculateGoalProgress(state: HomeUiState): Float {
    if (state.todayGoalCount <= 0) {
        return 0f
    }
    return (state.completedCount.toFloat() / state.todayGoalCount.toFloat()).coerceIn(0f, 1f)
}
