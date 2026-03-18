package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yueliangmanle.danci.core.database.entity.AiProviderProfileEntity

@Dao
interface AiProviderProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: AiProviderProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<AiProviderProfileEntity>)

    @Query("SELECT * FROM ai_provider_profiles ORDER BY updatedAt DESC, name ASC")
    suspend fun getAllProfiles(): List<AiProviderProfileEntity>

    @Query("SELECT * FROM ai_provider_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): AiProviderProfileEntity?

    @Delete
    suspend fun deleteProfile(profile: AiProviderProfileEntity)

    @Query("DELETE FROM ai_provider_profiles")
    suspend fun clearProfiles()
}
