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
                ),
                onStartNewWordsClick = {},
                onStartReviewClick = {},
                onOpenMistakesClick = {},
                onAnalyzePlanClick = {},
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
                ),
                onStartNewWordsClick = {},
                onStartReviewClick = {},
                onOpenMistakesClick = {},
                onAnalyzePlanClick = { clicked = true },
            )
        }

        composeRule.onNodeWithText("分析并调整计划").performClick()
        assertTrue(clicked)
    }
}
