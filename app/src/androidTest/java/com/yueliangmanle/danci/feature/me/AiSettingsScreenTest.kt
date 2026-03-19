package com.yueliangmanle.danci.feature.me

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
class AiSettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aiSettingsScreenShowsBaseUrlModelAndApiKeyEntry() {
        var saved = false

        composeRule.setContent {
            AiSettingsScreen(
                state = AiSettingsUiState(
                    isEnabled = true,
                    enablePlanAdjustments = true,
                    enableSessionCheckpoints = true,
                    editor = AiProfileEditorState(
                        name = "默认档案",
                        baseUrl = "https://api.openai.com/v1",
                        model = "gpt-5-mini",
                        apiKeyInput = "",
                        hasSavedApiKey = true,
                    ),
                ),
                onEnabledChange = {},
                onPlanAdjustmentsChange = {},
                onSessionCheckpointsChange = {},
                onDefaultProfileChange = {},
                onWordHelpProfileChange = {},
                onPlanAdjustmentProfileChange = {},
                onPhoneticFillProfileChange = {},
                onSelectProfile = {},
                onNewProfileClick = {},
                onEditorNameChange = {},
                onEditorBaseUrlChange = {},
                onEditorModelChange = {},
                onEditorApiKeyChange = {},
                onEditorEnabledChange = {},
                onSaveProfileClick = {},
                onClearApiKeyClick = {},
                onDeleteProfileClick = {},
                onSaveGlobalClick = { saved = true },
            )
        }

        composeRule.onNodeWithText("Base URL").assertIsDisplayed()
        composeRule.onNodeWithText("模型").assertIsDisplayed()
        composeRule.onNodeWithText("API Key").assertIsDisplayed()
        composeRule.onNodeWithText("保存全局路由与开关").performClick()
        assertTrue(saved)
    }
}
