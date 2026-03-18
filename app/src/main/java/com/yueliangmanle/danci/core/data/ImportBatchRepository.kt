package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.ImportBatchDao
import com.yueliangmanle.danci.core.database.entity.ImportBatchEntity
import com.yueliangmanle.danci.core.model.ImportBatch

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
