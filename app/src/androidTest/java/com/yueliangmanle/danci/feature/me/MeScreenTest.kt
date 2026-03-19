package com.yueliangmanle.danci.feature.me

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun meScreenShowsBackupReminderAndAiEntry() {
        composeRule.setContent {
            MeScreen(
                state = MeUiState(
                    reminderEnabled = true,
                    reminderTimeLabel = "21:30",
                    aiEnabled = true,
                    aiModel = "gpt-5-mini",
                    backupSummary = "最近备份：今天 19:40",
                ),
                onReminderEnabledChange = {},
                onAdjustReminderTimeClick = {},
                onExportBackupClick = {},
                onRestoreBackupClick = {},
                onOpenAiSettingsClick = {},
                onOpenPronunciationSettingsClick = {},
            )
        }

        composeRule.onNodeWithText("备份与恢复").assertIsDisplayed()
        composeRule.onNodeWithText("每日提醒").assertIsDisplayed()
        composeRule.onNodeWithText("AI 设置").assertIsDisplayed()
    }
}
