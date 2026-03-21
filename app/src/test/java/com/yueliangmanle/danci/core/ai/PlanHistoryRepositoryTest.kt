package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanHistoryRepositoryTest {
    private val repository = PlanHistoryRepository(
        nowProvider = { Instant.parse("2026-03-21T12:00:00Z") },
    )

    @Test
    fun createCandidateVersion_setsPendingConfirmationForMajorPlan() = runTest {
        val version = repository.createCandidateVersion(
            snapshot = snapshot(
                planHistory = listOf(
                    appliedPlan(
                        id = 3L,
                        recommendedFocus = listOf("abandon"),
                        suggestedModes = listOf("card", "quiz"),
                        suggestedPace = "steady",
                    ),
                ),
            ),
            adjustment = adjustment(
                recommendedFocus = listOf("precise"),
                suggestedModes = listOf("quiz", "dictation"),
                suggestedPace = "steady",
            ),
        )

        assertEquals(3L, version.parentPlanVersionId)
        assertEquals(PlanSeverity.MAJOR, version.severity)
        assertEquals(PlanApplyStatus.PENDING_CONFIRMATION, version.applyStatus)
        assertTrue(version.changeSummary.orEmpty().isNotBlank())
    }

    @Test
    fun createCandidateVersion_keepsRecentAppliedPlanAsComparisonBaseline() = runTest {
        val version = repository.createCandidateVersion(
            snapshot = snapshot(
                planHistory = listOf(
                    appliedPlan(
                        id = 3L,
                        generatedAt = "2026-03-21T08:00:00Z",
                        recommendedFocus = listOf("abandon"),
                        suggestedModes = listOf("card", "quiz"),
                        suggestedPace = "steady",
                    ),
                    pendingPlan(
                        id = 4L,
                        parentPlanVersionId = 3L,
                        generatedAt = "2026-03-21T09:00:00Z",
                    ),
                ),
            ),
            adjustment = adjustment(
                recommendedFocus = listOf("abandon"),
                suggestedModes = listOf("quiz", "word_detail"),
                suggestedPace = "slow_down",
            ),
        )

        assertEquals(3L, version.parentPlanVersionId)
    }

    @Test
    fun appendCandidateVersion_supersedesExistingPendingPlans() {
        val history = listOf(
            appliedPlan(id = 3L),
            pendingPlan(id = 4L, parentPlanVersionId = 3L),
        )
        val candidate = pendingPlan(
            id = 5L,
            parentPlanVersionId = 3L,
            generatedAt = "2026-03-21T11:00:00Z",
        )

        val updated = repository.appendCandidateVersion(history, candidate)

        assertEquals(PlanApplyStatus.SUPERSEDED, updated.first { it.id == 4L }.applyStatus)
        assertEquals(PlanApplyStatus.PENDING_CONFIRMATION, updated.first { it.id == 5L }.applyStatus)
    }

    @Test
    fun confirmPendingPlan_marksPlanAppliedAndSetsConfirmedAt() {
        val updated = repository.confirmPendingPlan(
            planHistory = listOf(
                appliedPlan(id = 3L),
                pendingPlan(id = 4L, parentPlanVersionId = 3L),
            ),
            planVersionId = 4L,
        )

        val confirmed = updated.first { it.id == 4L }
        assertEquals(PlanApplyStatus.APPLIED, confirmed.applyStatus)
        assertNotNull(confirmed.confirmedAt)
    }

    @Test
    fun rejectPendingPlan_marksPlanRejectedAndSetsRejectedAt() {
        val updated = repository.rejectPendingPlan(
            planHistory = listOf(
                appliedPlan(id = 3L),
                pendingPlan(id = 4L, parentPlanVersionId = 3L),
            ),
            planVersionId = 4L,
        )

        val rejected = updated.first { it.id == 4L }
        assertEquals(PlanApplyStatus.REJECTED, rejected.applyStatus)
        assertNotNull(rejected.rejectedAt)
    }

    private fun snapshot(planHistory: List<PlanHistoryEntry>): CurrentPlanSnapshot =
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
            memory = AiMemorySummary(planHistory = planHistory),
            activeBookTitle = "四级核心词",
            headline = "今天还要学 25 个词",
            mistakeCount = 6,
            anomalyNotes = listOf("最近错题升高"),
        )

    private fun adjustment(
        recommendedFocus: List<String>,
        suggestedModes: List<String>,
        suggestedPace: String?,
    ): AiPlanAdjustmentResult =
        AiPlanAdjustmentResult(
            summary = "收缩当前节奏，先清错词",
            recommendedFocus = recommendedFocus,
            suggestedModes = suggestedModes,
            suggestedPace = suggestedPace,
            checkpointAdvice = "先做一轮近义词辨析",
            reasonSummary = "最近近义词误判升高",
            changeSummary = null,
            abnormalSignals = listOf("连续错题升高"),
            executionEffect = "最近两次检查点正确率下降",
            source = PlanSource.AI,
        )

    private fun appliedPlan(
        id: Long,
        parentPlanVersionId: Long? = null,
        generatedAt: String = "2026-03-21T07:00:00Z",
        recommendedFocus: List<String> = listOf("abandon"),
        suggestedModes: List<String> = listOf("card", "quiz"),
        suggestedPace: String? = "steady",
    ): PlanHistoryEntry =
        PlanHistoryEntry(
            id = id,
            generatedAt = Instant.parse(generatedAt),
            summary = "当前执行计划",
            parentPlanVersionId = parentPlanVersionId,
            recommendedFocus = recommendedFocus,
            suggestedModes = suggestedModes,
            suggestedPace = suggestedPace,
            severity = PlanSeverity.MINOR,
            applyStatus = PlanApplyStatus.APPLIED,
            sourceType = "AI",
        )

    private fun pendingPlan(
        id: Long,
        parentPlanVersionId: Long?,
        generatedAt: String = "2026-03-21T10:00:00Z",
    ): PlanHistoryEntry =
        PlanHistoryEntry(
            id = id,
            generatedAt = Instant.parse(generatedAt),
            summary = "候选计划",
            parentPlanVersionId = parentPlanVersionId,
            recommendedFocus = listOf("precise"),
            suggestedModes = listOf("quiz", "word_detail"),
            suggestedPace = "slow_down",
            severity = PlanSeverity.MAJOR,
            applyStatus = PlanApplyStatus.PENDING_CONFIRMATION,
            sourceType = "AI",
        )
}
