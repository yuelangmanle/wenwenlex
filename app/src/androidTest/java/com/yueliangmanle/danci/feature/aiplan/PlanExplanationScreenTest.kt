package com.yueliangmanle.danci.feature.aiplan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanExplanationScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun explanationScreen_showsReasonAndChangeOnFirstPage() {
        composeRule.setContent {
            PlanExplanationScreen(
                state = PlanExplanationUiState(
                    headline = "建议回拉错词并降低新词推进速度。",
                    reasonSummary = "最近两次 checkpoint 都出现近义词误判升高。",
                    changeSummary = "把新词从 20 下调到 10，并增加 quiz + dictation。",
                    executionEffect = "预计能先把错词率压下来。",
                    abnormalSignals = listOf("近义词误判升高", "错词回看次数增加"),
                    sourceLabel = "AI 生成",
                ),
            )
        }

        composeRule.onNodeWithText("为什么改").assertIsDisplayed()
        composeRule.onNodeWithText("改了什么").assertIsDisplayed()
    }
}
