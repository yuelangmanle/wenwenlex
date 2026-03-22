package com.yueliangmanle.danci.feature.diagnostics

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
class DiagnosticsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun diagnosticsScreen_showsSummaryIssuesAndActions() {
        composeRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(
                    appVersionLabel = "版本 1.7 (170)",
                    checkedAtLabel = "03-22 18:40",
                    statusTitle = "需要处理",
                    summary = "最近一次完整性检查发现 2 个需要处理的问题。",
                    issues = listOf(
                        DiagnosticsIssueUiModel(
                            severityLabel = "提醒",
                            title = "缺少本地备份",
                            detail = "当前设备还没有最近备份。",
                        ),
                    ),
                    details = listOf(
                        DiagnosticsDetailUiModel("学习记录", "42"),
                    ),
                ),
                onRefreshClick = {},
                onExportClick = {},
            )
        }

        composeRule.onNodeWithText("诊断中心").assertIsDisplayed()
        composeRule.onNodeWithText("刷新诊断").assertIsDisplayed()
        composeRule.onNodeWithText("导出诊断包").assertIsDisplayed()
        composeRule.onNodeWithText("提醒 · 缺少本地备份").assertIsDisplayed()
        composeRule.onNodeWithText("当前快照").assertIsDisplayed()
    }

    @Test
    fun diagnosticsScreen_invokesExportAction() {
        var clicked = false

        composeRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(
                    appVersionLabel = "版本 1.7 (170)",
                    checkedAtLabel = "03-22 18:40",
                    summary = "最近一次完整性检查未发现关键问题。",
                ),
                onRefreshClick = {},
                onExportClick = { clicked = true },
            )
        }

        composeRule.onNodeWithText("导出诊断包").performClick()
        assertTrue(clicked)
    }
}
