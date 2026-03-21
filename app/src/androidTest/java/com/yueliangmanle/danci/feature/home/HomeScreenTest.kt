package com.yueliangmanle.danci.feature.home

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
class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeScreenShowsTodayStatsAndPrimaryActions() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    headline = "今天还要学 20 个词",
                    newWordCount = 8,
                    reviewCount = 12,
                    mistakeCount = 4,
                    estimatedMinutes = 17,
                    streakDays = 6,
                    activeBookTitle = "四级核心词",
                    planCenterTitle = "AI 计划中心",
                    planCenterSummary = "最近一次调整建议先回拉错词。",
                    planCenterMeta = "已生效 · 最近更新刚刚",
                ),
                onStartNewWordsClick = {},
                onStartReviewClick = {},
                onOpenMistakesClick = {},
                onAnalyzePlanClick = {},
                onOpenLearningAnalyticsClick = {},
                onOpenPlanCenterClick = {},
            )
        }

        composeRule.onNodeWithText("今天还要学 20 个词").assertIsDisplayed()
        composeRule.onNodeWithText("开始新词学习").assertIsDisplayed()
        composeRule.onNodeWithText("开始复习").assertIsDisplayed()
        composeRule.onNodeWithText("分析并调整计划").assertIsDisplayed()
    }

    @Test
    fun homeScreenInvokesAnalyzeAction() {
        var clicked = false

        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    headline = "今天还要学 20 个词",
                    newWordCount = 8,
                    reviewCount = 12,
                    mistakeCount = 4,
                    estimatedMinutes = 17,
                    streakDays = 6,
                    activeBookTitle = "四级核心词",
                    planCenterTitle = "AI 计划中心",
                    planCenterSummary = "最近一次调整建议先回拉错词。",
                ),
                onStartNewWordsClick = {},
                onStartReviewClick = {},
                onOpenMistakesClick = {},
                onAnalyzePlanClick = { clicked = true },
                onOpenLearningAnalyticsClick = {},
                onOpenPlanCenterClick = {},
            )
        }

        composeRule.onNodeWithText("分析并调整计划").performClick()
        assertTrue(clicked)
    }

    @Test
    fun homeScreen_showsPendingPlanBanner() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    headline = "今天还要学 20 个词",
                    activeBookTitle = "四级核心词",
                    planCenterTitle = "AI 计划中心",
                    planCenterSummary = "最近一次大调整等待确认。",
                    pendingPlanCount = 1,
                    planCenterMeta = "需要处理",
                ),
                onStartNewWordsClick = {},
                onStartReviewClick = {},
                onOpenMistakesClick = {},
                onAnalyzePlanClick = {},
                onOpenLearningAnalyticsClick = {},
                onOpenPlanCenterClick = {},
            )
        }

        composeRule.onNodeWithText("有 1 条待确认调整").assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsLearningAnalyticsEntry() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    headline = "今天还要学 20 个词",
                    activeBookTitle = "四级核心词",
                ),
                onStartNewWordsClick = {},
                onStartReviewClick = {},
                onOpenMistakesClick = {},
                onAnalyzePlanClick = {},
                onOpenLearningAnalyticsClick = {},
                onOpenPlanCenterClick = {},
            )
        }

        composeRule.onNodeWithText("查看学习统计").assertIsDisplayed()
    }
}
