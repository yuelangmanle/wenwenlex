package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.CheckpointSummary
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanContextCompactorTest {
    private val referenceTime: Instant = Instant.parse("2026-03-21T12:00:00Z")
    private val compactor = PlanContextCompactor()

    @Test
    fun compact_keepsRecentWindowsOnly() {
        val compacted = compactor.compact(
            checkpointSummaries = listOf(
                checkpointSummary(
                    checkpointId = "checkpoint-1",
                    windowStartAt = "2026-03-01T08:00:00Z",
                    windowEndAt = "2026-03-01T08:30:00Z",
                ),
                checkpointSummary(
                    checkpointId = "checkpoint-2",
                    windowStartAt = "2026-03-10T08:00:00Z",
                    windowEndAt = "2026-03-10T08:30:00Z",
                ),
                checkpointSummary(
                    checkpointId = "checkpoint-3",
                    windowStartAt = "2026-03-18T08:00:00Z",
                    windowEndAt = "2026-03-18T08:30:00Z",
                ),
                checkpointSummary(
                    checkpointId = "checkpoint-4",
                    windowStartAt = "2026-03-20T08:00:00Z",
                    windowEndAt = "2026-03-20T08:30:00Z",
                ),
            ),
            referenceTime = referenceTime,
        )

        assertEquals(listOf("checkpoint-2", "checkpoint-3", "checkpoint-4"), compacted.map(CheckpointSummary::checkpointId))
    }

    @Test
    fun compact_preservesDecisionStatus() {
        val compacted = compactor.compact(
            checkpointSummaries = listOf(
                checkpointSummary(
                    checkpointId = "checkpoint-1",
                    windowStartAt = "2026-03-18T08:00:00Z",
                    windowEndAt = "2026-03-18T08:30:00Z",
                    decisionStatus = PlanApplyStatus.REJECTED,
                ),
            ),
            referenceTime = referenceTime,
        )

        assertEquals(PlanApplyStatus.REJECTED, compacted.single().decisionStatus)
    }

    private fun checkpointSummary(
        checkpointId: String,
        windowStartAt: String,
        windowEndAt: String,
        decisionStatus: PlanApplyStatus = PlanApplyStatus.APPLIED,
    ): CheckpointSummary =
        CheckpointSummary(
            checkpointId = checkpointId,
            windowStartAt = Instant.parse(windowStartAt),
            windowEndAt = Instant.parse(windowEndAt),
            effectivePlanVersionId = 7L,
            candidatePlanVersionId = 8L,
            decisionStatus = decisionStatus,
            effectSummary = "正确率回升",
            signalSummary = "近义词误判升高",
        )
}
