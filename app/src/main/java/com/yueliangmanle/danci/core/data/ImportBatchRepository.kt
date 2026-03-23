package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.ImportBatchDao
import com.yueliangmanle.danci.core.database.entity.ImportBatchEntity
import com.yueliangmanle.danci.core.model.ImportBatch
import com.yueliangmanle.danci.core.model.ImportDiagnosisSnapshot
import org.json.JSONObject

interface ImportBatchRepository {
    suspend fun insert(batch: ImportBatch): Long
    suspend fun getAll(): List<ImportBatch>
}

class RoomImportBatchRepository(
    private val dao: ImportBatchDao,
) : ImportBatchRepository {
    override suspend fun insert(batch: ImportBatch): Long = dao.insertBatch(batch.asEntity())

    override suspend fun getAll(): List<ImportBatch> = dao.getAllBatches().map(ImportBatchEntity::asExternalModel)
}

internal fun ImportBatch.asEntity(): ImportBatchEntity =
    ImportBatchEntity(
        id = id,
        bookId = bookId,
        fileName = fileName,
        sheetName = sheetName,
        diagnosisSnapshotJson = diagnosisSnapshot?.toJsonString(),
        parserMode = parserMode,
        totalRows = totalRows,
        importedRows = importedRows,
        skippedRows = skippedRows,
        aiNormalizedCount = aiNormalizedCount,
        aiCompletedCount = aiCompletedCount,
        createdAt = createdAt,
    )

internal fun ImportBatchEntity.asExternalModel(): ImportBatch =
    ImportBatch(
        id = id,
        bookId = bookId,
        fileName = fileName,
        sheetName = sheetName,
        diagnosisSnapshot = diagnosisSnapshotJson.toImportDiagnosisSnapshotOrNull(),
        parserMode = parserMode,
        totalRows = totalRows,
        importedRows = importedRows,
        skippedRows = skippedRows,
        aiNormalizedCount = aiNormalizedCount,
        aiCompletedCount = aiCompletedCount,
        createdAt = createdAt,
    )

fun buildImportBatchRepository(context: Context): ImportBatchRepository =
    RoomImportBatchRepository(buildDanciDatabase(context.applicationContext).importBatchDao())

private fun ImportDiagnosisSnapshot.toJsonString(): String =
    JSONObject()
        .put("inferred_structure_confidence", inferredStructureConfidence)
        .put("auto_fix_count", autoFixCount)
        .put("ai_fix_count", aiFixCount)
        .put("high_risk_issue_count", highRiskIssueCount)
        .put("final_imported_row_count", finalImportedRowCount)
        .toString()

private fun String?.toImportDiagnosisSnapshotOrNull(): ImportDiagnosisSnapshot? {
    val raw = this?.takeIf(String::isNotBlank) ?: return null
    return runCatching {
        val json = JSONObject(raw)
        ImportDiagnosisSnapshot(
            inferredStructureConfidence = json.optDouble("inferred_structure_confidence", 0.0).toFloat(),
            autoFixCount = json.optInt("auto_fix_count", 0),
            aiFixCount = json.optInt("ai_fix_count", 0),
            highRiskIssueCount = json.optInt("high_risk_issue_count", 0),
            finalImportedRowCount = json.optInt("final_imported_row_count", 0),
        )
    }.getOrNull()
}
