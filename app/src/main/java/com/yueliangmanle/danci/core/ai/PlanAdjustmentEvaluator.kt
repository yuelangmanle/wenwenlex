package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity

class PlanAdjustmentEvaluator {
    fun evaluate(
        previous: PlanHistoryEntry?,
        candidate: PlanHistoryEntry,
    ): PlanSeverity {
        if (previous == null) {
            return PlanSeverity.MINOR
        }

        var changedDimensions = 0
        if (previous.suggestedPace != candidate.suggestedPace) {
            changedDimensions += 1
        }
        if (previous.suggestedModes != candidate.suggestedModes) {
            changedDimensions += 1
        }
        if (previous.recommendedFocus != candidate.recommendedFocus) {
            changedDimensions += 1
        }
        return if (changedDimensions >= 2) {
            PlanSeverity.MAJOR
        } else {
            PlanSeverity.MINOR
        }
    }
}
