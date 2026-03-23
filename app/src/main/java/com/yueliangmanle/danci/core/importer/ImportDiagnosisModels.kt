package com.yueliangmanle.danci.core.importer

enum class ImportIssueSeverity {
    MINOR,
    SEVERE,
}

enum class ImportIssueType {
    WHITESPACE,
    CHINESE_COMMA,
    SHIFTED_WORD_COLUMN,
    SHIFTED_MEANING_COLUMN,
}

data class ImportIssue(
    val sheetName: String,
    val rowIndex: Int,
    val severity: ImportIssueSeverity,
    val type: ImportIssueType,
    val message: String,
)

data class ImportDiagnosis(
    val issues: List<ImportIssue> = emptyList(),
    val autoFixableCount: Int = 0,
    val severeIssueCount: Int = 0,
    val requiresAiDecision: Boolean = false,
    val inferredStructureConfidence: Float = 1f,
)

data class ImportRepairResult(
    val diagnosis: ImportDiagnosis,
    val preview: ImportPreview,
    val repairedSheets: List<XlsxSheetData>,
    val aiUsed: Boolean,
    val autoRepairSummary: String? = null,
)

internal fun ImportDiagnosis.toSummary(): String =
    when {
        severeIssueCount > 0 && autoFixableCount > 0 ->
            "检测到 $severeIssueCount 处高风险结构问题，已先自动修复 $autoFixableCount 处轻微问题。"
        severeIssueCount > 0 ->
            "检测到 $severeIssueCount 处高风险结构问题，建议先做 AI 修复。"
        autoFixableCount > 0 ->
            "检测到 $autoFixableCount 处轻微问题，已在本地自动修复。"
        else -> "未发现明显结构问题，可以直接检查预览并导入。"
    }
