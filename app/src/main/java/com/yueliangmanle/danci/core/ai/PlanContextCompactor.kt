package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.CheckpointSummary
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.StudyEvent
import java.time.Instant
import java.time.temporal.ChronoUnit

class PlanContextCompactor(
    private val maxCheckpointCount: Int = 3,
    private val retentionDays: Long = 30,
) {
    fun compact(
        checkpointSummaries: List<CheckpointSummary>,
        referenceTime: Instant = Instant.now(),
    ): List<CheckpointSummary> {
        val cutoff = referenceTime.minus(retentionDays, ChronoUnit.DAYS)
        return checkpointSummaries
            .sortedBy(CheckpointSummary::windowEndAt)
            .filter { summary -> !summary.windowEndAt.isBefore(cutoff) }
            .takeLast(maxCheckpointCount)
    }

    fun buildCheckpointSummaries(
        events: List<StudyEvent>,
        planHistory: List<PlanHistoryEntry>,
        existingSummaries: List<CheckpointSummary> = emptyList(),
        referenceTime: Instant = events.maxOfOrNull(StudyEvent::happenedAt) ?: Instant.now(),
    ): List<CheckpointSummary> {
        if (existingSummaries.isNotEmpty()) {
            return compact(existingSummaries, referenceTime)
        }
        if (planHistory.isEmpty()) {
            return emptyList()
        }

        val sortedPlans = planHistory.sortedBy(PlanHistoryEntry::generatedAt)
        val summaries = sortedPlans.mapIndexed { index, plan ->
            val nextGeneratedAt = sortedPlans.getOrNull(index + 1)?.generatedAt ?: referenceTime
            val windowEvents = events.filter { event ->
                !event.happenedAt.isBefore(plan.generatedAt) && event.happenedAt.isBefore(nextGeneratedAt)
            }
            val windowEndAt = windowEvents.maxOfOrNull(StudyEvent::happenedAt) ?: nextGeneratedAt
            val effectSummary = plan.executionEffect ?: windowEvents.effectSummary()
            val signalSummary = plan.abnormalSignals.joinToString("；").ifBlank { windowEvents.signalSummary() }
            CheckpointSummary(
                checkpointId = checkpointIdFor(plan, index),
                windowStartAt = plan.generatedAt,
                windowEndAt = windowEndAt,
                effectivePlanVersionId = plan.effectivePlanVersionId(),
                candidatePlanVersionId = plan.candidatePlanVersionId(),
                decisionStatus = plan.applyStatus,
                effectSummary = effectSummary,
                signalSummary = signalSummary,
            )
        }
        return compact(summaries, referenceTime)
    }

    private fun checkpointIdFor(
        plan: PlanHistoryEntry,
        index: Int,
    ): String {
        val stableId = plan.id.takeIf { it > 0 } ?: index.toLong() + 1
        return "checkpoint-$stableId-${plan.generatedAt.toEpochMilli()}"
    }
}

private fun PlanHistoryEntry.effectivePlanVersionId(): Long? =
    when (applyStatus) {
        PlanApplyStatus.APPLIED, PlanApplyStatus.SUPERSEDED -> id.takeIf { it > 0 }
        PlanApplyStatus.PENDING_CONFIRMATION, PlanApplyStatus.REJECTED -> parentPlanVersionId
    }

private fun PlanHistoryEntry.candidatePlanVersionId(): Long? =
    id.takeIf { it > 0 }

private fun List<StudyEvent>.effectSummary(): String {
    val answerEvents = filter { it.isCorrect != null }
    if (answerEvents.isEmpty()) {
        return "执行数据积累中"
    }
    val correctCount = answerEvents.count { it.isCorrect == true }
    val correctRate = (correctCount * 100) / answerEvents.size
    return "窗口内复习 ${answerEvents.size} 次，正确率 ${correctRate}%"
}

private fun List<StudyEvent>.signalSummary(): String {
    val wrongCount = count { it.isCorrect == false }
    return when {
        wrongCount >= 3 -> "连续错题升高"
        wrongCount > 0 -> "阶段错题有抬头"
        isEmpty() -> "阶段窗口更新"
        else -> "学习表现稳定"
    }
}
