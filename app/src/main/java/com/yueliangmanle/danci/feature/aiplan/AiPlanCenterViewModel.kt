package com.yueliangmanle.danci.feature.aiplan

import android.content.Context
import com.yueliangmanle.danci.core.ai.PlanExplanationComposer
import com.yueliangmanle.danci.core.ai.PlanHistoryRepository
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanEffectSnapshot
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val AI_PLAN_CENTER_EMPTY_MESSAGE = "先学习一段时间，AI 才能形成稳定调整历史"

data class AiPlanCenterUiState(
    val isLoading: Boolean = false,
    val currentPlanId: Long? = null,
    val currentPlanSummary: String? = null,
    val currentPlanMeta: String? = null,
    val latestPlanEffectTitle: String? = null,
    val latestPlanEffectSummary: String? = null,
    val pendingPlan: PlanHistoryEntry? = null,
    val timeline: List<PlanHistoryEntry> = emptyList(),
    val emptyMessage: String? = AI_PLAN_CENTER_EMPTY_MESSAGE,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class AiPlanCenterViewModel(
    private val studyRepository: StudyRepository,
    private val planHistoryRepository: PlanHistoryRepository,
    private val explanationComposer: PlanExplanationComposer = PlanExplanationComposer(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    suspend fun loadUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): AiPlanCenterUiState = withContext(Dispatchers.IO) {
        val memorySummary = studyRepository.loadAiMemorySummary(planLimit = 50)
        val history = memorySummary.planHistory.sortedByDescending(PlanHistoryEntry::generatedAt)
        val pendingPlan = history.firstOrNull { it.applyStatus == PlanApplyStatus.PENDING_CONFIRMATION }
        val currentPlan = history.firstOrNull { it.applyStatus == PlanApplyStatus.APPLIED }
        val latestPlanEffect = memorySummary.analyticsSnapshot.planEffects.firstOrNull()

        AiPlanCenterUiState(
            currentPlanId = currentPlan?.id,
            currentPlanSummary = currentPlan?.summary,
            currentPlanMeta = currentPlan?.let(::buildPlanMeta),
            latestPlanEffectTitle = latestPlanEffect?.let { "最近调整效果" },
            latestPlanEffectSummary = latestPlanEffect?.toSummary(),
            pendingPlan = pendingPlan,
            timeline = history.filterNot { it.id == pendingPlan?.id },
            emptyMessage = if (history.isEmpty()) AI_PLAN_CENTER_EMPTY_MESSAGE else null,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun confirmPendingPlan(planVersionId: Long): AiPlanCenterUiState {
        planHistoryRepository.confirmPendingPlan(planVersionId)
        return loadUiState(statusMessage = "已应用这次 AI 调整。")
    }

    suspend fun rejectPendingPlan(planVersionId: Long): AiPlanCenterUiState {
        planHistoryRepository.rejectPendingPlan(planVersionId)
        return loadUiState(statusMessage = "这次 AI 调整已归档为历史记录。")
    }

    private fun buildPlanMeta(entry: PlanHistoryEntry): String {
        val timestamp = (entry.confirmedAt ?: entry.generatedAt).atZone(zoneId).format(timeFormatter)
        return "${explanationComposer.composeDecisionLabel(entry)} · $timestamp"
    }

    private fun PlanEffectSnapshot.toSummary(): String {
        val beforeRate = beforeCorrectRate?.let(::formatPercent) ?: "样本不足"
        val afterRate = afterCorrectRate?.let(::formatPercent) ?: "样本不足"
        val outcome = outcomeSummary ?: "效果仍在观察"
        return "${label.ifBlank { "最近一次调整" }}：$outcome，调整前 $beforeRate，调整后 $afterRate。"
    }

    private fun formatPercent(value: Float): String = "${(value * 100).roundToInt()}%"
}

suspend fun loadAiPlanCenterViewModel(context: Context): AiPlanCenterViewModel =
    withContext(Dispatchers.IO) {
        val studyRepository = RoomStudyRepository(buildDanciDatabase(context.applicationContext).studyDao())
        AiPlanCenterViewModel(
            studyRepository = studyRepository,
            planHistoryRepository = PlanHistoryRepository(studyRepository = studyRepository),
        )
    }
