package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yueliangmanle.danci.core.database.entity.WordAudioAssetEntity

@Dao
interface WordAudioAssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsset(asset: WordAudioAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssets(assets: List<WordAudioAssetEntity>)

    @Query(
        """
        SELECT * FROM word_audio_assets
        WHERE wordId = :wordId AND accent = :accent AND sourceType = :sourceType
        ORDER BY COALESCE(lastPlayedAt, fetchedAt) DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestAsset(
        wordId: Long,
        accent: String,
        sourceType: String,
    ): WordAudioAssetEntity?

    @Query(
        """
        SELECT * FROM word_audio_assets
        WHERE wordId = :wordId AND sourceType = :sourceType AND status = :status
        ORDER BY COALESCE(lastPlayedAt, fetchedAt) DESC, id DESC
        """,
    )
    suspend fun findAssetsForWord(
        wordId: Long,
        sourceType: String,
        status: String,
    ): List<WordAudioAssetEntity>

    @Query(
        """
        SELECT * FROM word_audio_assets
        WHERE wordId = :wordId AND accent = :accent AND sourceType = :sourceType AND status = :status
        ORDER BY COALESCE(lastPlayedAt, fetchedAt) DESC, id DESC
        """,
    )
    suspend fun findAssetsForWordAccentAndSource(
        wordId: Long,
        accent: String,
        sourceType: String,
        status: String,
    ): List<WordAudioAssetEntity>

    @Query(
        """
        SELECT * FROM word_audio_assets
        WHERE sourceType = :sourceType AND status = :status
        ORDER BY COALESCE(lastPlayedAt, fetchedAt) ASC, id ASC
        """,
    )
    suspend fun getAssetsBySource(
        sourceType: String,
        status: String,
    ): List<WordAudioAssetEntity>

    @Query("DELETE FROM word_audio_assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)

    @Query("DELETE FROM word_audio_assets WHERE sourceType = :sourceType")
    suspend fun deleteAssetsBySource(sourceType: String)

    @Query("DELETE FROM word_audio_assets")
    suspend fun clearAssets()
}
