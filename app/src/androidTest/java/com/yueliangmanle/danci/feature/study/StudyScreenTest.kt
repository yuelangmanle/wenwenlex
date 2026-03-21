package com.yueliangmanle.danci.feature.study

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun studyScreenShowsWordAndThreeFeedbackButtons() {
        composeRule.setContent {
            StudyScreen(
                state = StudyUiState(
                    currentWord = "abandon",
                    phonetic = "/əˈbændən/",
                    meanings = listOf("放弃"),
                    progressText = "1 / 10",
                ),
                onFeedbackClick = {},
                onOpenDetailClick = {},
                onPlayPronunciationClick = {},
                onOpenPlanCenterClick = {},
            )
        }

        composeRule.onNodeWithText("abandon").assertIsDisplayed()
        composeRule.onNodeWithText("不认识").assertIsDisplayed()
        composeRule.onNodeWithText("模糊").assertIsDisplayed()
        composeRule.onNodeWithText("认识").assertIsDisplayed()
    }

    @Test
    fun studyScreenShowsDetailedPronunciationStatusMessage() {
        composeRule.setContent {
            StudyScreen(
                state = StudyUiState(
                    currentWord = "abandon",
                    progressText = "1 / 10",
                    statusMessage = "已联网获取英式词典音频（有道词典）。",
                    checkpointDecisionLabel = "需要确认",
                    checkpointTitle = "AI 阶段建议",
                    checkpointSuggestion = "建议先暂停新词推进。",
                    canOpenPlanCenter = true,
                ),
                onFeedbackClick = {},
                onOpenDetailClick = {},
                onPlayPronunciationClick = {},
                onOpenPlanCenterClick = {},
            )
        }

        composeRule.onNodeWithText("已联网获取英式词典音频（有道词典）。").assertIsDisplayed()
        composeRule.onNodeWithText("需要确认").assertIsDisplayed()
        composeRule.onNodeWithText("查看 AI 计划中心").assertIsDisplayed()
    }
}
