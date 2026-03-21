package com.yueliangmanle.danci.feature.aiplan

import android.content.Context
import com.yueliangmanle.danci.core.ai.PlanExplanationComposer
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlanExplanationUiState(
    val headline: String = "",
    val reasonSummary: String = "",
    val changeSummary: String = "",
    val executionEffect: String? = null,
    val abnormalSignals: List<String> = emptyList(),
    val sourceLabel: String = "",
    val decisionLabel: String = "",
    val errorMessage: String? = null,
)

class PlanExplanationViewModel(
    private val studyRepository: StudyRepository,
    private val composer: PlanExplanationComposer = PlanExplanationComposer(),
) {
    suspend fun loadUiState(planVersionId: Long): PlanExplanationUiState = withContext(Dispatchers.IO) {
        val entry = studyRepository.loadAiMemorySummary(planLimit = 100)
            .planHistory
            .sortedByDescending(PlanHistoryEntry::generatedAt)
            .firstOrNull { it.id == planVersionId }
            ?: return@withContext PlanExplanationUiState(
                errorMessage = "暂时找不到这条计划记录。",
            )

        PlanExplanationUiState(
            headline = composer.composeHeadline(entry),
            reasonSummary = composer.composeReason(entry),
            changeSummary = entry.changeSummary ?: "本次没有改动核心节奏，只补充了执行说明。",
            executionEffect = entry.executionEffect,
            abnormalSignals = entry.abnormalSignals,
            sourceLabel = if (entry.sourceType.equals("AI", ignoreCase = true)) {
                "AI 生成"
            } else {
                "本地兜底"
            },
            decisionLabel = composer.composeDecisionLabel(entry),
        )
    }
}

suspend fun loadPlanExplanationViewModel(context: Context): PlanExplanationViewModel =
    withContext(Dispatchers.IO) {
        PlanExplanationViewModel(
            studyRepository = RoomStudyRepository(buildDanciDatabase(context.applicationContext).studyDao()),
        )
    }
