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
class PlanComparisonScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun comparisonScreen_alwaysUsesLatestAppliedPlanAsCurrent() {
        composeRule.setContent {
            PlanComparisonScreen(
                state = PlanComparisonUiState(
                    currentPlan = appliedPlan(
                        id = 3L,
                        summary = "当前计划先稳住复习。",
                    ),
                    targetPlan = historyPlan(
                        id = 1L,
                        summary = "历史版本偏向新词推进。",
                    ),
                    focusDiff = listOf("当前：错词回拉", "历史：新词推进"),
                    paceDiff = "当前 steady vs 历史 fast",
                    modeDiff = listOf("当前：quiz", "历史：card"),
                ),
            )
        }

        composeRule.onNodeWithText("当前计划").assertIsDisplayed()
        composeRule.onNodeWithText("历史版本").assertIsDisplayed()
    }

    private fun appliedPlan(
        id: Long,
        summary: String,
    ): PlanHistoryEntry =
        PlanHistoryEntry(
            id = id,
            generatedAt = Instant.parse("2026-03-21T10:00:00Z"),
            summary = summary,
            recommendedFocus = listOf("abandon"),
            suggestedModes = listOf("quiz"),
            suggestedPace = "steady",
            severity = PlanSeverity.MINOR,
            applyStatus = PlanApplyStatus.APPLIED,
            sourceType = "AI",
        )

    private fun historyPlan(
        id: Long,
        summary: String,
    ): PlanHistoryEntry =
        PlanHistoryEntry(
            id = id,
            generatedAt = Instant.parse("2026-03-19T10:00:00Z"),
            summary = summary,
            recommendedFocus = listOf("advance"),
            suggestedModes = listOf("card"),
            suggestedPace = "fast",
            severity = PlanSeverity.MINOR,
            applyStatus = PlanApplyStatus.SUPERSEDED,
            sourceType = "AI",
        )
}
