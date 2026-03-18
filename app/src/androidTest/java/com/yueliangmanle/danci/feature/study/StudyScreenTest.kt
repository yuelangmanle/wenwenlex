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
            )
        }

        composeRule.onNodeWithText("abandon").assertIsDisplayed()
        composeRule.onNodeWithText("不认识").assertIsDisplayed()
        composeRule.onNodeWithText("模糊").assertIsDisplayed()
        composeRule.onNodeWithText("认识").assertIsDisplayed()
    }
}
