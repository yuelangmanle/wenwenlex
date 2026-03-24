package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yueliangmanle.danci.core.database.entity.AudioGenerationJobEntity

@Dao
interface AudioGenerationJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: AudioGenerationJobEntity): Long

    @Update
    suspend fun updateJob(job: AudioGenerationJobEntity)

    @Query("SELECT * FROM audio_generation_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobById(jobId: Long): AudioGenerationJobEntity?

    @Query("SELECT * FROM audio_generation_jobs ORDER BY updatedAt DESC, id DESC")
    suspend fun getAllJobs(): List<AudioGenerationJobEntity>

    @Query("DELETE FROM audio_generation_jobs")
    suspend fun clearJobs()
}
