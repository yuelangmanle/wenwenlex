package com.yueliangmanle.danci.core.model

import java.time.Instant

data class ImportBatch(
    val id: Long = 0,
    val bookId: String,
    val fileName: String,
    val sheetName: String? = null,
    val parserMode: String = "strict",
    val totalRows: Int = 0,
    val importedRows: Int = 0,
    val skippedRows: Int = 0,
    val aiNormalizedCount: Int = 0,
    val aiCompletedCount: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
)
