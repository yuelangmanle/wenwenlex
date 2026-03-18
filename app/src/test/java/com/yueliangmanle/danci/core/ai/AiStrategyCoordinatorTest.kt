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
