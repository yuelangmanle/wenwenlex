package com.yueliangmanle.danci.feature.worddetail

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordDetailAiActionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wordDetailAiActionsShowResultCards() {
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    WordDetailUiState(
                        word = "abandon",
                        meanings = listOf("放弃"),
                    ),
                )
            }
            WordDetailScreen(
                state = state,
                onAiMemoryClick = {
                    state = state.copy(
                        aiCards = listOf(
                            AiInsightCardUiState(
                                title = "记忆提示",
                                body = "把 abandon 想成 a-band-on，像把乐队临时解散。",
                            ),
                        ),
                    )
                },
                onAiContrastClick = {},
                onExplainWordFormsClick = {},
                onExpandExampleClick = {},
                onStartQuizClick = {},
            )
        }

        composeRule.onNodeWithText("AI 助记").performClick()
        composeRule.onNodeWithText("记忆提示").assertIsDisplayed()
    }
}
