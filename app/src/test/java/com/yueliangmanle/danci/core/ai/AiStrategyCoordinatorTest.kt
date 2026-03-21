package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.model.AiMemorySummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiStrategyCoordinatorTest {
    @Test
    fun ignoresInvalidSuggestionAndKeepsLocalPlan() = runTest {
        val coordinator = AiStrategyCoordinator(
            client = FakeAiClient(response = "{bad json"),
        )

        val result = coordinator.adjustPlan(sampleCurrentPlan())

        assertEquals(PlanSource.LOCAL_FALLBACK, result.source)
        assertTrue(result.summary.isNotBlank())
        assertTrue(result.reasonSummary.orEmpty().isNotBlank())
        assertTrue(result.abnormalSignals.isNotEmpty())
    }

    @Test
    fun parsesStructuredPlanAdjustmentFields() = runTest {
        val coordinator = AiStrategyCoordinator(
            client = FakeAiClient(
                response = """
                    {
                      "summary":"先收缩新词推进",
                      "recommended_focus":["abandon","precise"],
                      "suggested_modes":["quiz","dictation"],
                      "suggested_pace":"slow_down",
                      "checkpoint_advice":"先稳住正确率",
                      "reason_summary":"连续错题升高",
                      "change_summary":"从卡片切到测验和听写",
                      "abnormal_signals":["连续错题升高","近义词误判增多"],
                      "execution_effect":"待观察"
                    }
                """.trimIndent(),
            ),
        )

        val result = coordinator.adjustPlan(sampleCurrentPlan())

        assertEquals(PlanSource.AI, result.source)
        assertEquals("连续错题升高", result.reasonSummary)
        assertEquals("从卡片切到测验和听写", result.changeSummary)
        assertEquals(listOf("连续错题升高", "近义词误判增多"), result.abnormalSignals)
        assertEquals("待观察", result.executionEffect)
    }

    private fun sampleCurrentPlan(): CurrentPlanSnapshot =
        CurrentPlanSnapshot(
            settings = AppSettings(
                dailyGoal = 25,
                activeBookId = "cet4",
                aiEnabled = true,
                aiBaseUrl = "https://api.openai.com/v1",
                aiModel = "gpt-5-mini",
                aiPlanAdjustmentEnabled = true,
                aiSessionCheckpointEnabled = true,
            ),
            runtimeSettings = AiRuntimeSettings(
                enabled = true,
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-test",
                model = "gpt-5-mini",
            ),
            memory = AiMemorySummary(),
            activeBookTitle = "四级核心词",
            headline = "今天还要学 25 个词",
            mistakeCount = 6,
            anomalyNotes = listOf("最近错题升高"),
        )
}

private class FakeAiClient(
    private val response: String,
) : AiClient {
    override suspend fun generate(request: AiTextRequest): AiTextResponse =
        AiTextResponse(
            text = response,
            rawBody = response,
        )
}
