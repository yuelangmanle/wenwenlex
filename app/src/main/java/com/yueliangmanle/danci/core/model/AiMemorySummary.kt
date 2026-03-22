package com.yueliangmanle.danci.core.model

import java.time.Instant

data class AiMemorySummary(
    val learnerProfile: LearnerProfile? = null,
    val dailySummaries: List<DailySummary> = emptyList(),
    val weeklySummaries: List<WeeklySummary> = emptyList(),
    val planHistory: List<PlanHistoryEntry> = emptyList(),
    val checkpointSummaries: List<CheckpointSummary> = emptyList(),
    val analyticsSnapshot: LearningAnalyticsSnapshot = LearningAnalyticsSnapshot(),
    val longTermInsights: List<String> = emptyList(),
    val confusionEdges: List<ConfusionEdge> = emptyList(),
    val goalProgress: GoalProgressSnapshot = GoalProgressSnapshot(),
    val upgradeHealth: Map<String, String> = emptyMap(),
)

data class DailySummary(
    val date: String,
    val studiedCount: Int,
    val reviewCount: Int,
    val correctRate: Float,
    val fatigueNote: String? = null,
    val primaryMistakeReasons: List<String> = emptyList(),
    val updatedAt: Instant = Instant.EPOCH,
)

data class WeeklySummary(
    val weekStartDate: String,
    val studiedCount: Int,
    val correctRate: Float,
    val trendSummary: String? = null,
    val persistentWeakSpots: List<String> = emptyList(),
    val updatedAt: Instant = Instant.EPOCH,
)

data class LearnerProfile(
    val profileId: String = DEFAULT_PROFILE_ID,
    val vocabularyLevel: String? = null,
    val weakSpots: List<String> = emptyList(),
    val preferredQuestionTypes: List<String> = emptyList(),
    val commonMistakePatterns: List<String> = emptyList(),
    val updatedAt: Instant = Instant.EPOCH,
) {
    companion object {
        const val DEFAULT_PROFILE_ID = "default"
    }
}

data class PlanHistoryEntry(
    val id: Long = 0,
    val generatedAt: Instant,
    val summary: String,
    val parentPlanVersionId: Long? = null,
    val triggerType: String = DEFAULT_TRIGGER_TYPE,
    val sourceType: String = DEFAULT_SOURCE_TYPE,
    val recommendedFocus: List<String> = emptyList(),
    val suggestedModes: List<String> = emptyList(),
    val suggestedPace: String? = null,
    val reasonSummary: String? = null,
    val changeSummary: String? = null,
    val abnormalSignals: List<String> = emptyList(),
    val severity: PlanSeverity = PlanSeverity.MINOR,
    val applyStatus: PlanApplyStatus = PlanApplyStatus.APPLIED,
    val isHighlightedAiChange: Boolean = false,
    val executionEffect: String? = null,
    val confirmedAt: Instant? = null,
    val rejectedAt: Instant? = null,
) {
    companion object {
        const val DEFAULT_TRIGGER_TYPE = "manual_refresh"
        const val DEFAULT_SOURCE_TYPE = "LOCAL_FALLBACK"
    }
}

enum class PlanSeverity {
    MINOR,
    MAJOR,
}

enum class PlanApplyStatus {
    APPLIED,
    PENDING_CONFIRMATION,
    REJECTED,
    SUPERSEDED,
}

data class CheckpointSummary(
    val checkpointId: String,
    val windowStartAt: Instant,
    val windowEndAt: Instant,
    val effectivePlanVersionId: Long? = null,
    val candidatePlanVersionId: Long? = null,
    val decisionStatus: PlanApplyStatus = PlanApplyStatus.APPLIED,
    val effectSummary: String,
    val signalSummary: String,
)

data class ConfusionEdge(
    val sourceWordId: Long,
    val targetWordId: Long,
    val relationType: String,
    val weight: Float = 0f,
    val mistakeCount: Int = 0,
    val updatedAt: Instant = Instant.EPOCH,
)
