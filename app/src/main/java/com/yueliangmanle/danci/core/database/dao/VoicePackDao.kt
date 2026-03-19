package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity

@Dao
interface VoicePackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVoicePack(voicePack: VoicePackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVoicePacks(voicePacks: List<VoicePackEntity>)

    @Query("SELECT * FROM voice_packs ORDER BY isActive DESC, updatedAt DESC, name ASC")
    suspend fun getAllVoicePacks(): List<VoicePackEntity>

    @Query("SELECT * FROM voice_packs WHERE id = :id LIMIT 1")
    suspend fun getVoicePackById(id: String): VoicePackEntity?

    @Query("SELECT * FROM voice_packs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveVoicePack(): VoicePackEntity?

    @Query("UPDATE voice_packs SET isActive = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun activateVoicePack(id: String)

    @Query("UPDATE voice_packs SET isActive = 0")
    suspend fun clearActiveVoicePack()

    @Query("DELETE FROM voice_packs WHERE id = :id")
    suspend fun deleteVoicePackById(id: String)

    @Query("DELETE FROM voice_packs")
    suspend fun clearVoicePacks()
}
