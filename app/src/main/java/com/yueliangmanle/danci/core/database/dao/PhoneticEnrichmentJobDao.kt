package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yueliangmanle.danci.core.database.entity.PhoneticEnrichmentJobEntity

@Dao
interface PhoneticEnrichmentJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: PhoneticEnrichmentJobEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<PhoneticEnrichmentJobEntity>)

    @Update
    suspend fun updateJob(job: PhoneticEnrichmentJobEntity)

    @Query("SELECT * FROM phonetic_enrichment_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobById(jobId: Long): PhoneticEnrichmentJobEntity?

    @Query("SELECT * FROM phonetic_enrichment_jobs ORDER BY updatedAt DESC")
    suspend fun getAllJobs(): List<PhoneticEnrichmentJobEntity>

    @Query("DELETE FROM phonetic_enrichment_jobs")
    suspend fun clearJobs()
}
