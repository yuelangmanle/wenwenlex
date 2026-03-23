package com.yueliangmanle.danci.core.model

data class ImportDiagnosisSnapshot(
    val inferredStructureConfidence: Float = 0f,
    val autoFixCount: Int = 0,
    val aiFixCount: Int = 0,
    val highRiskIssueCount: Int = 0,
    val finalImportedRowCount: Int = 0,
)
