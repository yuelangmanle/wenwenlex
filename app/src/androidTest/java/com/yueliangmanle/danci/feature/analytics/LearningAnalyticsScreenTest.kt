package com.yueliangmanle.danci.feature.analytics

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningAnalyticsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun analyticsScreen_showsOverviewAndHtmlPlaceholderCopy() {
        composeRule.setContent {
            LearningAnalyticsScreen(
                state = LearningAnalyticsUiState(
                    title = "学习统计",
                    overviewCards = listOf(
                        AnalyticsCardUiModel("正确率", "78%"),
                        AnalyticsCardUiModel("学习天数", "7"),
                    ),
                    chartHtml = null,
                    insightBullets = listOf("最近一次调整后正确率回升。"),
                ),
                onRefreshClick = {},
            )
        }

        composeRule.onNodeWithText("学习统计").assertIsDisplayed()
        composeRule.onNodeWithText("正确率").assertIsDisplayed()
        composeRule.onNodeWithText("HTML 统计看板").assertIsDisplayed()
        composeRule.onNodeWithText("暂时还没有可展示的统计图表。").assertIsDisplayed()
        composeRule.onNodeWithText("长期摘要").assertIsDisplayed()
    }

    @Test
    fun analyticsScreen_invokesRefreshAction() {
        var clicked = false

        composeRule.setContent {
            LearningAnalyticsScreen(
                state = LearningAnalyticsUiState(
                    overviewCards = listOf(AnalyticsCardUiModel("今日完成", "28")),
                    chartHtml = "<html><body>chart</body></html>",
                ),
                onRefreshClick = { clicked = true },
            )
        }

        composeRule.onNodeWithText("刷新统计").performClick()
        assertTrue(clicked)
    }

    @Test
    fun analyticsScreen_showsGoalProgressSection() {
        composeRule.setContent {
            LearningAnalyticsScreen(
                state = LearningAnalyticsUiState(
                    overviewCards = listOf(AnalyticsCardUiModel("正确率", "78%")),
                    goalProgressCards = listOf(
                        AnalyticsCardUiModel("本周目标", "18 / 40"),
                        AnalyticsCardUiModel("当前阶段", "六级冲刺 · 320 / 1200"),
                    ),
                ),
                onRefreshClick = {},
            )
        }

        composeRule.onNodeWithText("目标推进").assertIsDisplayed()
        composeRule.onNodeWithText("本周目标").assertIsDisplayed()
        composeRule.onNodeWithText("当前阶段").assertIsDisplayed()
    }
}
