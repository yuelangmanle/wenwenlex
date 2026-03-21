package com.yueliangmanle.danci.core.database.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "'manual_refresh'")
    val triggerType: String = com.yueliangmanle.danci.core.model.PlanHistoryEntry.DEFAULT_TRIGGER_TYPE,
    @ColumnInfo(defaultValue = "'LOCAL_FALLBACK'")
    val sourceType: String = com.yueliangmanle.danci.core.model.PlanHistoryEntry.DEFAULT_SOURCE_TYPE,
    val recommendedFocus: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "''")
    val suggestedModes: List<String> = emptyList(),
    val suggestedPace: String? = null,
    val reasonSummary: String? = null,
    val changeSummary: String? = null,
    @ColumnInfo(defaultValue = "''")
    val abnormalSignals: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "'MINOR'")
    val severity: com.yueliangmanle.danci.core.model.PlanSeverity = com.yueliangmanle.danci.core.model.PlanSeverity.MINOR,
    @ColumnInfo(defaultValue = "'APPLIED'")
    val applyStatus: com.yueliangmanle.danci.core.model.PlanApplyStatus = com.yueliangmanle.danci.core.model.PlanApplyStatus.APPLIED,
    @ColumnInfo(defaultValue = "0")
    val isHighlightedAiChange: Boolean = false,
    val executionEffect: String? = null,
    val confirmedAt: Instant? = null,
    val rejectedAt: Instant? = null,
)
