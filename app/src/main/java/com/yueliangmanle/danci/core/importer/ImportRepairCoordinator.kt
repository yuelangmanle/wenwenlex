package com.yueliangmanle.danci.core.importer

import com.yueliangmanle.danci.core.ai.AiRuntimeSettings

class ImportRepairCoordinator(
    private val diagnosisEngine: ImportDiagnosisEngine = ImportDiagnosisEngine(),
    private val aiNormalize: suspend (List<XlsxSheetData>, AiRuntimeSettings?) -> ImportPreview = { sheets, runtimeSettings ->
        AiImportNormalizer().normalize(sheets, runtimeSettings)
    },
) {
    suspend fun repair(
        workbook: List<XlsxSheetData>,
        allowAi: Boolean,
        runtimeSettings: AiRuntimeSettings?,
    ): ImportRepairResult {
        val diagnosis = diagnosisEngine.diagnose(workbook)
        val repairedSheets = diagnosisEngine.applyMinorFixes(workbook)
        val localPreview = diagnosisEngine.preview(repairedSheets).copy(
            parserMode = if (diagnosis.autoFixableCount > 0) "local_repaired" else "strict",
        )
        val autoRepairSummary = diagnosisEngine.buildAutoRepairSummary(diagnosis)
        if (!diagnosis.requiresAiDecision || !allowAi) {
            return ImportRepairResult(
                diagnosis = diagnosis,
                preview = localPreview,
                repairedSheets = repairedSheets,
                aiUsed = false,
                autoRepairSummary = autoRepairSummary,
            )
        }

        val aiPreview = aiNormalize(repairedSheets, runtimeSettings)
        return ImportRepairResult(
            diagnosis = diagnosis,
            preview = aiPreview,
            repairedSheets = repairedSheets,
            aiUsed = true,
            autoRepairSummary = autoRepairSummary,
        )
    }
}
