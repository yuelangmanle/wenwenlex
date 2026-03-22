package com.yueliangmanle.danci.feature.me

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
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
                onOpenGoalSettingsClick = {},
                onOpenAiSettingsClick = {},
                onOpenLearningAnalyticsClick = {},
                onOpenPronunciationSettingsClick = {},
            )
        }

        composeRule.onNodeWithText("备份与恢复").assertIsDisplayed()
        composeRule.onNodeWithText("每日提醒").assertIsDisplayed()
        composeRule.onNodeWithText("AI 设置").assertIsDisplayed()
        composeRule.onNodeWithText("AI 计划记录").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun meScreen_showsLearningAnalyticsEntry() {
        composeRule.setContent {
            MeScreen(
                state = MeUiState(),
                onReminderEnabledChange = {},
                onAdjustReminderTimeClick = {},
                onExportBackupClick = {},
                onRestoreBackupClick = {},
                onOpenGoalSettingsClick = {},
                onOpenAiSettingsClick = {},
                onOpenLearningAnalyticsClick = {},
                onOpenPronunciationSettingsClick = {},
            )
        }

        composeRule.onNodeWithText("学习统计").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun meScreen_invokesLearningAnalyticsAction() {
        var clicked = false

        composeRule.setContent {
            MeScreen(
                state = MeUiState(),
                onReminderEnabledChange = {},
                onAdjustReminderTimeClick = {},
                onExportBackupClick = {},
                onRestoreBackupClick = {},
                onOpenGoalSettingsClick = {},
                onOpenAiSettingsClick = {},
                onOpenLearningAnalyticsClick = { clicked = true },
                onOpenPronunciationSettingsClick = {},
            )
        }

        composeRule.onNodeWithText("打开学习统计").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun meScreen_showsGoalSettingsEntry() {
        composeRule.setContent {
            MeScreen(
                state = MeUiState(
                    dailyGoal = 20,
                    weeklyGoal = 70,
                    phaseName = "六级冲刺",
                    phaseTargetWords = 1200,
                    phaseCompletedWords = 480,
                ),
                onReminderEnabledChange = {},
                onAdjustReminderTimeClick = {},
                onExportBackupClick = {},
                onRestoreBackupClick = {},
                onOpenGoalSettingsClick = {},
                onOpenAiSettingsClick = {},
                onOpenLearningAnalyticsClick = {},
                onOpenPronunciationSettingsClick = {},
            )
        }

        composeRule.onNodeWithText("目标设置与阶段管理").assertIsDisplayed()
        composeRule.onNodeWithText("打开目标设置").performScrollTo().assertIsDisplayed()
    }
}
