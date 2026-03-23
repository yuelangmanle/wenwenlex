package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yueliangmanle.danci.core.database.entity.AudioGenerationTaskEntity
import com.yueliangmanle.danci.core.database.entity.AudioGenerationTaskItemEntity

@Dao
interface AudioGenerationTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<AudioGenerationTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskItems(items: List<AudioGenerationTaskItemEntity>)

    @Query("SELECT * FROM audio_generation_tasks ORDER BY updatedAt DESC, id DESC")
    suspend fun getAllTasks(): List<AudioGenerationTaskEntity>

    @Query("SELECT * FROM audio_generation_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): AudioGenerationTaskEntity?

    @Query("SELECT * FROM audio_generation_task_items WHERE taskId = :taskId ORDER BY itemKey ASC")
    suspend fun getItemsByTask(taskId: String): List<AudioGenerationTaskItemEntity>

    @Query("DELETE FROM audio_generation_task_items")
    suspend fun clearTaskItems()

    @Query("DELETE FROM audio_generation_tasks")
    suspend fun clearTasks()
}
