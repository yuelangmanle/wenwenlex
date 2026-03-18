package com.yueliangmanle.danci.feature.worddetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordDetailScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wordDetailShowsRelationsFormsAndAiActions() {
        composeRule.setContent {
            WordDetailScreen(
                state = WordDetailUiState(
                    word = "abandon",
                    phonetic = "/əˈbændən/",
                    meanings = listOf("放弃"),
                    synonyms = listOf("give up"),
                    antonyms = listOf("continue"),
                    similarWords = listOf("abundant"),
                    wordForms = listOf("abandoned", "abandoning"),
                    root = "bandon",
                ),
                onAiMemoryClick = {},
                onAiContrastClick = {},
                onStartQuizClick = {},
            )
        }

        composeRule.onNodeWithText("近义词").assertIsDisplayed()
        composeRule.onNodeWithText("反义词").assertIsDisplayed()
        composeRule.onNodeWithText("拼写相近词").assertIsDisplayed()
        composeRule.onNodeWithText("单词变形").assertIsDisplayed()
        composeRule.onNodeWithText("AI 助记").assertIsDisplayed()
    }
}
