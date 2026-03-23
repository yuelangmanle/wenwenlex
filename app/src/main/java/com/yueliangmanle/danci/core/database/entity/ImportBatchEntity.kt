package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val fileName: String,
    val sheetName: String? = null,
    val diagnosisSnapshotJson: String? = null,
    val parserMode: String = "strict",
    val totalRows: Int = 0,
    val importedRows: Int = 0,
    val skippedRows: Int = 0,
    val aiNormalizedCount: Int = 0,
    val aiCompletedCount: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
)
