package com.yueliangmanle.danci.feature.aiplan

import android.content.Context
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlanComparisonUiState(
    val currentPlan: PlanHistoryEntry? = null,
    val targetPlan: PlanHistoryEntry? = null,
    val focusDiff: List<String> = emptyList(),
    val paceDiff: String? = null,
    val modeDiff: List<String> = emptyList(),
    val executionEffectDiff: String? = null,
    val errorMessage: String? = null,
)

class PlanComparisonViewModel(
    private val studyRepository: StudyRepository,
) {
    suspend fun loadUiState(planVersionId: Long): PlanComparisonUiState = withContext(Dispatchers.IO) {
        val history = studyRepository.loadAiMemorySummary(planLimit = 100)
            .planHistory
            .sortedByDescending(PlanHistoryEntry::generatedAt)
        val currentPlan = history.firstOrNull { it.applyStatus == PlanApplyStatus.APPLIED }
        val targetPlan = history.firstOrNull { it.id == planVersionId }

        if (currentPlan == null || targetPlan == null) {
            return@withContext PlanComparisonUiState(
                currentPlan = currentPlan,
                targetPlan = targetPlan,
                errorMessage = "暂时找不到可对比的计划版本。",
            )
        }

        PlanComparisonUiState(
            currentPlan = currentPlan,
            targetPlan = targetPlan,
            focusDiff = buildDiffRows("重点", currentPlan.recommendedFocus, targetPlan.recommendedFocus),
            paceDiff = if (currentPlan.suggestedPace == targetPlan.suggestedPace) {
                "节奏一致：${targetPlan.suggestedPace ?: "未设置"}"
            } else {
                "当前 ${currentPlan.suggestedPace ?: "未设置"} -> 历史 ${targetPlan.suggestedPace ?: "未设置"}"
            },
            modeDiff = buildDiffRows("模式", currentPlan.suggestedModes, targetPlan.suggestedModes),
            executionEffectDiff = targetPlan.executionEffect ?: currentPlan.executionEffect,
        )
    }
}

suspend fun loadPlanComparisonViewModel(context: Context): PlanComparisonViewModel =
    withContext(Dispatchers.IO) {
        PlanComparisonViewModel(
            studyRepository = RoomStudyRepository(buildDanciDatabase(context.applicationContext).studyDao()),
        )
    }

private fun buildDiffRows(
    label: String,
    currentValues: List<String>,
    targetValues: List<String>,
): List<String> =
    listOf(
        "当前$label：${currentValues.joinToString("、").ifBlank { "未设置" }}",
        "历史$label：${targetValues.joinToString("、").ifBlank { "未设置" }}",
    )
