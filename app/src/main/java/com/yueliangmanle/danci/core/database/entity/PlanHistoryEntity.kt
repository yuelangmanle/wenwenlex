package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "plan_history",
    indices = [Index("generatedAt")],
)
data class PlanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val generatedAt: Instant,
    val summary: String,
    val parentPlanVersionId: Long? = null,
    val triggerType: String? = null,
    val sourceType: String? = null,
    val recommendedFocus: List<String> = emptyList(),
    val suggestedModes: List<String> = emptyList(),
    val suggestedPace: String? = null,
    val reasonSummary: String? = null,
    val changeSummary: String? = null,
    val abnormalSignals: List<String> = emptyList(),
    val severity: com.yueliangmanle.danci.core.model.PlanSeverity = com.yueliangmanle.danci.core.model.PlanSeverity.MINOR,
    val applyStatus: com.yueliangmanle.danci.core.model.PlanApplyStatus = com.yueliangmanle.danci.core.model.PlanApplyStatus.APPLIED,
    val isHighlightedAiChange: Boolean = false,
    val executionEffect: String? = null,
    val confirmedAt: Instant? = null,
    val rejectedAt: Instant? = null,
)
