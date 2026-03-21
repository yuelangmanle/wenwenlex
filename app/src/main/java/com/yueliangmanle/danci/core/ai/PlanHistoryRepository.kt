package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import java.time.Instant

data class PersistedPlanAdjustment(
    val adjustment: AiPlanAdjustmentResult,
    val version: PlanHistoryEntry,
)

class PlanHistoryRepository(
    private val studyRepository: StudyRepository? = null,
    private val evaluator: PlanAdjustmentEvaluator = PlanAdjustmentEvaluator(),
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    suspend fun createCandidateVersion(
        snapshot: CurrentPlanSnapshot,
        adjustment: AiPlanAdjustmentResult,
    ): PlanHistoryEntry {
        val memorySummary = studyRepository?.loadAiMemorySummary(planLimit = 50) ?: snapshot.memory
        val currentApplied = memorySummary.planHistory.lastAppliedPlan()
        val generatedAt = nowProvider()
        val baseDraft = PlanHistoryEntry(
            generatedAt = generatedAt,
            summary = adjustment.summary,
            parentPlanVersionId = currentApplied?.id,
            triggerType = snapshot.triggerType(),
            sourceType = adjustment.source.name,
            recommendedFocus = adjustment.recommendedFocus,
            suggestedModes = adjustment.suggestedModes,
            suggestedPace = adjustment.suggestedPace,
            reasonSummary = adjustment.reasonSummary,
            changeSummary = adjustment.changeSummary,
            abnormalSignals = adjustment.abnormalSignals,
            executionEffect = adjustment.executionEffect,
        )
        val draft = baseDraft.copy(
            changeSummary = adjustment.changeSummary ?: deriveChangeSummary(currentApplied, baseDraft),
        )
        val severity = evaluator.evaluate(currentApplied, draft)
        val applyStatus = if (severity == PlanSeverity.MAJOR) {
            PlanApplyStatus.PENDING_CONFIRMATION
        } else {
            PlanApplyStatus.APPLIED
        }
        val candidate = draft.copy(
            severity = severity,
            applyStatus = applyStatus,
            isHighlightedAiChange = adjustment.source == PlanSource.AI || severity == PlanSeverity.MAJOR,
            confirmedAt = if (applyStatus == PlanApplyStatus.APPLIED) generatedAt else null,
        )

        val updatedPlanHistory = appendCandidateVersion(memorySummary.planHistory, candidate)
        if (studyRepository == null) {
            return candidate
        }
        studyRepository.saveAiMemorySummary(memorySummary.copy(planHistory = updatedPlanHistory))

        return studyRepository.loadAiMemorySummary(planLimit = updatedPlanHistory.size + 5)
            .planHistory
            .sortedByDescending(PlanHistoryEntry::generatedAt)
            .firstOrNull { it.generatedAt == generatedAt && it.summary == candidate.summary }
            ?: candidate
    }

    suspend fun persistAdjustment(
        snapshot: CurrentPlanSnapshot,
        adjustment: AiPlanAdjustmentResult,
    ): PersistedPlanAdjustment =
        PersistedPlanAdjustment(
            adjustment = adjustment,
            version = createCandidateVersion(snapshot, adjustment),
        )

    fun appendCandidateVersion(
        planHistory: List<PlanHistoryEntry>,
        candidate: PlanHistoryEntry,
    ): List<PlanHistoryEntry> =
        planHistory.map { existing ->
            when {
                existing.applyStatus == PlanApplyStatus.PENDING_CONFIRMATION ->
                    existing.copy(applyStatus = PlanApplyStatus.SUPERSEDED)
                candidate.applyStatus == PlanApplyStatus.APPLIED && existing.applyStatus == PlanApplyStatus.APPLIED ->
                    existing.copy(applyStatus = PlanApplyStatus.SUPERSEDED)
                else -> existing
            }
        } + candidate

    fun confirmPendingPlan(
        planHistory: List<PlanHistoryEntry>,
        planVersionId: Long,
    ): List<PlanHistoryEntry> {
        val confirmedAt = nowProvider()
        return planHistory.map { entry ->
            when {
                entry.id == planVersionId -> entry.copy(
                    applyStatus = PlanApplyStatus.APPLIED,
                    confirmedAt = confirmedAt,
                    rejectedAt = null,
                )
                entry.applyStatus == PlanApplyStatus.APPLIED -> entry.copy(
                    applyStatus = PlanApplyStatus.SUPERSEDED,
                )
                else -> entry
            }
        }
    }

    fun rejectPendingPlan(
        planHistory: List<PlanHistoryEntry>,
        planVersionId: Long,
    ): List<PlanHistoryEntry> {
        val rejectedAt = nowProvider()
        return planHistory.map { entry ->
            if (entry.id == planVersionId) {
                entry.copy(
                    applyStatus = PlanApplyStatus.REJECTED,
                    rejectedAt = rejectedAt,
                    confirmedAt = null,
                )
            } else {
                entry
            }
        }
    }
}

private fun List<PlanHistoryEntry>.lastAppliedPlan(): PlanHistoryEntry? =
    filter { it.applyStatus == PlanApplyStatus.APPLIED }
        .maxWithOrNull(compareBy<PlanHistoryEntry>({ it.generatedAt }, { it.id }))

private fun CurrentPlanSnapshot.triggerType(): String =
    when {
        anomalyNotes.isNotEmpty() || mistakeCount > 0 -> "study_checkpoint"
        headline.contains("分析") -> "home_analysis"
        else -> "manual_refresh"
    }

private fun deriveChangeSummary(
    previous: PlanHistoryEntry?,
    candidate: PlanHistoryEntry,
): String =
    buildList {
        if (previous?.suggestedPace != candidate.suggestedPace) {
            add("节奏调整为 ${candidate.suggestedPace ?: "未指定"}")
        }
        if (previous?.suggestedModes != candidate.suggestedModes) {
            add("题型切换为 ${candidate.suggestedModes.joinToString("、").ifBlank { "未指定" }}")
        }
        if (previous?.recommendedFocus != candidate.recommendedFocus) {
            add("重点改为 ${candidate.recommendedFocus.joinToString("、").ifBlank { "未指定" }}")
        }
    }.joinToString("；").ifBlank { "沿用当前计划，仅补充细节说明。" }
