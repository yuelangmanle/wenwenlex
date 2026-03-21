package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry

class PlanExplanationComposer {
    fun composeHeadline(entry: PlanHistoryEntry): String = entry.summary

    fun composeReason(entry: PlanHistoryEntry): String =
        entry.reasonSummary ?: entry.abnormalSignals.joinToString("；").ifBlank { "当前以稳态调整为主。" }

    fun composeDecisionLabel(entry: PlanHistoryEntry): String =
        when (entry.applyStatus) {
            PlanApplyStatus.APPLIED -> "已生效"
            PlanApplyStatus.PENDING_CONFIRMATION -> "待确认"
            PlanApplyStatus.REJECTED -> "已拒绝"
            PlanApplyStatus.SUPERSEDED -> "已覆盖"
        }
}
