package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yueliangmanle.danci.core.database.entity.ImportBatchEntity

@Dao
interface ImportBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: ImportBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<ImportBatchEntity>)

    @Query("SELECT * FROM import_batches ORDER BY createdAt DESC")
    suspend fun getAllBatches(): List<ImportBatchEntity>

    @Query("DELETE FROM import_batches")
    suspend fun clearBatches()
}
