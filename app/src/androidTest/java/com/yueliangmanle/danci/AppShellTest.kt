package com.yueliangmanle.danci

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsOnHomeAndShowsFourTabs() {
        composeRule.onAllNodesWithText("首页").assertCountEquals(1)
        composeRule.onNodeWithText("首页").assertIsDisplayed()
        composeRule.onNodeWithText("学习").assertIsDisplayed()
        composeRule.onNodeWithText("词书").assertIsDisplayed()
        composeRule.onNodeWithText("我的").assertIsDisplayed()
    }
}
