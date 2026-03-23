package com.yueliangmanle.danci.feature.study

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.yueliangmanle.danci.core.study.StudyQueueEmptyState
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
                onBackHomeClick = {},
                onPlayPronunciationClick = {},
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
                ),
                onFeedbackClick = {},
                onOpenDetailClick = {},
                onBackHomeClick = {},
                onPlayPronunciationClick = {},
            )
        }

        composeRule.onNodeWithText("已联网获取英式词典音频（有道词典）。").assertIsDisplayed()
    }

    @Test
    fun studyScreenShowsLoadingStateBeforeQueueReady() {
        composeRule.setContent {
            StudyScreen(
                state = StudyUiState(),
                onFeedbackClick = {},
                onOpenDetailClick = {},
                onBackHomeClick = {},
                onPlayPronunciationClick = {},
            )
        }

        composeRule.onNodeWithText("正在准备本轮单词…").assertIsDisplayed()
    }

    @Test
    fun studyScreenShowsModeSpecificEmptyStateMessage() {
        composeRule.setContent {
            StudyScreen(
                state = StudyUiState(
                    isLoadingQueue = false,
                    emptyState = StudyQueueEmptyState.NO_RECENT_MISTAKES,
                ),
                onFeedbackClick = {},
                onOpenDetailClick = {},
                onBackHomeClick = {},
                onPlayPronunciationClick = {},
            )
        }

        composeRule.onNodeWithText("最近没有需要回拉的错词").assertIsDisplayed()
        composeRule.onNodeWithText("返回首页").assertIsDisplayed()
    }
}
