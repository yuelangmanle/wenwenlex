package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yueliangmanle.danci.core.database.entity.PronunciationSourceEntity
import com.yueliangmanle.danci.core.database.entity.PronunciationSourcePresetEntity

@Dao
interface PronunciationSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSources(sources: List<PronunciationSourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPresets(presets: List<PronunciationSourcePresetEntity>)

    @Query("SELECT * FROM pronunciation_sources ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllSources(): List<PronunciationSourceEntity>

    @Query("SELECT * FROM pronunciation_source_presets WHERE sourceId = :sourceId ORDER BY presetId ASC")
    suspend fun getPresetsBySource(sourceId: String): List<PronunciationSourcePresetEntity>

    @Query("DELETE FROM pronunciation_source_presets WHERE sourceId IN (:sourceIds)")
    suspend fun deletePresetsBySourceIds(sourceIds: List<String>)

    @Query("DELETE FROM pronunciation_source_presets")
    suspend fun clearPresets()

    @Query("DELETE FROM pronunciation_sources")
    suspend fun clearSources()
}
