package com.yueliangmanle.danci.feature.aiplan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiPlanCenterScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aiPlanCenter_showsPendingConfirmationSectionFirst() {
        composeRule.setContent {
            AiPlanCenterScreen(
                state = AiPlanCenterUiState(
                    currentPlanSummary = "当前计划：每天 20 词，先稳住复习。",
                    pendingPlan = samplePendingPlan(),
                    timeline = listOf(samplePendingPlan(), sampleAppliedPlan()),
                ),
                onConfirmPendingPlanClick = { _ -> },
                onRejectPendingPlanClick = { _ -> },
                onOpenComparisonClick = { _ -> },
                onOpenExplanationClick = { _ -> },
            )
        }

        composeRule.onNodeWithText("待确认调整").assertIsDisplayed()
        composeRule.onNodeWithText("确认应用").assertIsDisplayed()
    }

    @Test
    fun aiPlanCenter_showsEmptyStateWhenNoHistory() {
        composeRule.setContent {
            AiPlanCenterScreen(
                state = AiPlanCenterUiState(),
                onConfirmPendingPlanClick = { _ -> },
                onRejectPendingPlanClick = { _ -> },
                onOpenComparisonClick = { _ -> },
                onOpenExplanationClick = { _ -> },
            )
        }

        composeRule.onNodeWithText("先学习一段时间，AI 才能形成稳定调整历史").assertIsDisplayed()
    }

    @Test
    fun aiPlanCenter_showsLatestPlanEffectSummary() {
        composeRule.setContent {
            AiPlanCenterScreen(
                state = AiPlanCenterUiState(
                    currentPlanSummary = "当前计划：每天 20 词，先稳住复习。",
                    latestPlanEffectTitle = "最近调整效果",
                    latestPlanEffectSummary = "先回拉错词后，正确率从 65% 回升到 73%。",
                ),
                onConfirmPendingPlanClick = { _ -> },
                onRejectPendingPlanClick = { _ -> },
                onOpenComparisonClick = { _ -> },
                onOpenExplanationClick = { _ -> },
            )
        }

        composeRule.onNodeWithText("最近调整效果").assertIsDisplayed()
        composeRule.onNodeWithText("先回拉错词后，正确率从 65% 回升到 73%。").assertIsDisplayed()
    }

    private fun samplePendingPlan(): PlanHistoryEntry =
        PlanHistoryEntry(
            id = 12L,
            generatedAt = Instant.parse("2026-03-21T13:00:00Z"),
            summary = "建议暂停新词两天，回拉错词和近义词辨析。",
            suggestedModes = listOf("quiz", "dictation"),
            recommendedFocus = listOf("abandon", "abundant"),
            suggestedPace = "slow_down",
            severity = PlanSeverity.MAJOR,
            applyStatus = PlanApplyStatus.PENDING_CONFIRMATION,
            reasonSummary = "最近近义词误判升高。",
            changeSummary = "减少新词，增加复习比重。",
            sourceType = "AI",
        )

    private fun sampleAppliedPlan(): PlanHistoryEntry =
        PlanHistoryEntry(
            id = 9L,
            generatedAt = Instant.parse("2026-03-20T13:00:00Z"),
            summary = "当前计划保持稳态推进。",
            suggestedModes = listOf("card", "quiz"),
            recommendedFocus = listOf("abandon"),
            suggestedPace = "steady",
            severity = PlanSeverity.MINOR,
            applyStatus = PlanApplyStatus.APPLIED,
            sourceType = "AI",
        )
}
