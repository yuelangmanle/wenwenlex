package com.yueliangmanle.danci.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun LearningAnalyticsScreen(
    state: LearningAnalyticsUiState,
    onRefreshClick: () -> Unit,
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
                Text("正在整理最近学习统计")
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
        HeroCard(
            title = state.title,
            subtitle = state.subtitle,
            onRefreshClick = onRefreshClick,
        )
        OverviewGrid(cards = state.overviewCards)
        HtmlBoardCard(html = state.chartHtml)
        InsightCard(insights = state.insightBullets)
        state.errorMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    onRefreshClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF8EEE5),
                            Color(0xFFE9F0F7),
                            Color(0xFFF5D7C4),
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = onRefreshClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("刷新统计")
            }
        }
    }
}

@Composable
private fun OverviewGrid(cards: List<AnalyticsCardUiModel>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "核心概览",
            style = MaterialTheme.typography.titleMedium,
        )
        cards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowCards.forEach { card ->
                    OverviewCard(
                        card = card,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowCards.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    card: AnalyticsCardUiModel,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = card.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = card.value,
                style = MaterialTheme.typography.headlineSmall,
            )
            card.supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HtmlBoardCard(html: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "HTML 统计看板",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "通过内置 HTML 看板查看趋势、反馈分布、计划效果和发音使用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (html.isNullOrBlank()) {
                Text(
                    text = "暂时还没有可展示的统计图表。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LearningAnalyticsWebView(
                    html = html,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                )
            }
        }
    }
}

@Composable
private fun InsightCard(insights: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "长期摘要",
                style = MaterialTheme.typography.titleMedium,
            )
            insights.forEach { insight ->
                Text(
                    text = "- $insight",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
