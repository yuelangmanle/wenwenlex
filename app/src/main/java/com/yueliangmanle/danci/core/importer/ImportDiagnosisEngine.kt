package com.yueliangmanle.danci.core.importer

class ImportDiagnosisEngine(
    private val importer: XlsxBookImporter = XlsxBookImporter(),
) {
    fun diagnose(workbook: List<XlsxSheetData>): ImportDiagnosis {
        val issues = buildList {
            workbook.forEach { sheet ->
                sheet.rows.forEachIndexed { index, row ->
                    addAll(
                        diagnoseRow(
                            sheetName = sheet.name,
                            rowIndex = index,
                            row = row,
                        ),
                    )
                }
            }
        }
        val autoFixableCount = issues.count { it.severity == ImportIssueSeverity.MINOR }
        val severeIssueCount = issues.count { it.severity == ImportIssueSeverity.SEVERE }
        return ImportDiagnosis(
            issues = issues,
            autoFixableCount = autoFixableCount,
            severeIssueCount = severeIssueCount,
            requiresAiDecision = severeIssueCount > 0,
            inferredStructureConfidence = calculateStructureConfidence(
                minorIssueCount = autoFixableCount,
                severeIssueCount = severeIssueCount,
            ),
        )
    }

    fun applyMinorFixes(workbook: List<XlsxSheetData>): List<XlsxSheetData> =
        workbook.map { sheet ->
            sheet.copy(
                rows = sheet.rows.map { row ->
                    row.map(String::trim)
                },
            )
        }

    fun preview(workbook: List<XlsxSheetData>): ImportPreview {
        val previews = workbook.map(importer::preview)
        return previews.maxByOrNull { it.rows.size }
            ?: ImportPreview(
                sheetName = "Sheet1",
                rows = emptyList(),
                totalRows = 0,
                skippedRows = 0,
                warningMessage = "没有可识别的数据。",
            )
    }

    fun buildAutoRepairSummary(diagnosis: ImportDiagnosis): String? {
        if (diagnosis.autoFixableCount <= 0) {
            return null
        }
        val actions = linkedSetOf<String>()
        diagnosis.issues.forEach { issue ->
            when (issue.type) {
                ImportIssueType.WHITESPACE -> actions += "首尾空格"
                ImportIssueType.CHINESE_COMMA -> actions += "中英文逗号"
                else -> Unit
            }
        }
        return if (actions.isEmpty()) {
            "已自动修复 ${diagnosis.autoFixableCount} 处轻微问题。"
        } else {
            "已自动修复 ${diagnosis.autoFixableCount} 处轻微问题：${actions.joinToString("、")}。"
        }
    }

    private fun diagnoseRow(
        sheetName: String,
        rowIndex: Int,
        row: List<String>,
    ): List<ImportIssue> {
        val trimmedRow = row.map(String::trim)
        val first = trimmedRow.getOrNull(0).orEmpty()
        val second = trimmedRow.getOrNull(1).orEmpty()
        if (looksLikeHeader(first, second)) {
            return emptyList()
        }

        val issues = mutableListOf<ImportIssue>()
        if (row.any { it != it.trim() && it.isNotBlank() }) {
            issues += ImportIssue(
                sheetName = sheetName,
                rowIndex = rowIndex,
                severity = ImportIssueSeverity.MINOR,
                type = ImportIssueType.WHITESPACE,
                message = "单元格前后有多余空格。",
            )
        }
        if (second.contains('，')) {
            issues += ImportIssue(
                sheetName = sheetName,
                rowIndex = rowIndex,
                severity = ImportIssueSeverity.MINOR,
                type = ImportIssueType.CHINESE_COMMA,
                message = "中文释义里混用了中文逗号。",
            )
        }
        if (looksShiftedWordRow(trimmedRow)) {
            issues += ImportIssue(
                sheetName = sheetName,
                rowIndex = rowIndex,
                severity = ImportIssueSeverity.SEVERE,
                type = ImportIssueType.SHIFTED_WORD_COLUMN,
                message = "单词列疑似被整体右移，当前前两列无法稳定解析。",
            )
        } else if (looksShiftedMeaningRow(trimmedRow)) {
            issues += ImportIssue(
                sheetName = sheetName,
                rowIndex = rowIndex,
                severity = ImportIssueSeverity.SEVERE,
                type = ImportIssueType.SHIFTED_MEANING_COLUMN,
                message = "中文释义疑似落在后续列，当前前两列无法稳定解析。",
            )
        }
        return issues
    }

    private fun looksShiftedWordRow(row: List<String>): Boolean {
        val first = row.getOrNull(0).orEmpty()
        val second = row.getOrNull(1).orEmpty()
        val thirdOrLaterHasMeaning = row.drop(2).any(String::isNotBlank)
        return first.isBlank() &&
            looksLikeWord(second) &&
            thirdOrLaterHasMeaning
    }

    private fun looksShiftedMeaningRow(row: List<String>): Boolean {
        val first = row.getOrNull(0).orEmpty()
        val second = row.getOrNull(1).orEmpty()
        val thirdOrLaterHasMeaning = row.drop(2).any(String::isNotBlank)
        return looksLikeWord(first) &&
            second.isBlank() &&
            thirdOrLaterHasMeaning
    }

    private fun looksLikeHeader(
        first: String,
        second: String,
    ): Boolean {
        val firstLower = first.lowercase()
        val secondLower = second.lowercase()
        return firstLower in setOf("word", "words", "单词", "英文", "english") ||
            secondLower in setOf("meaning", "meanings", "translation", "中文", "释义")
    }

    private fun looksLikeWord(value: String): Boolean =
        value.matches(Regex("[A-Za-z][A-Za-z\\- '\\u2019]*"))

    private fun calculateStructureConfidence(
        minorIssueCount: Int,
        severeIssueCount: Int,
    ): Float {
        val raw = 1f - (minorIssueCount * 0.08f) - (severeIssueCount * 0.35f)
        return raw.coerceIn(0.1f, 1f)
    }
}
