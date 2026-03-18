package com.yueliangmanle.danci.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yueliangmanle.danci.core.database.entity.ConfusionEdgeEntity
import com.yueliangmanle.danci.core.database.entity.DailySummaryEntity
import com.yueliangmanle.danci.core.database.entity.LearnerProfileEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.PlanHistoryEntity
import com.yueliangmanle.danci.core.database.entity.StudyEventEntity
import com.yueliangmanle.danci.core.database.entity.StudySessionEntity
import com.yueliangmanle.danci.core.database.entity.WeeklySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearningRecord(record: LearningRecordEntity)

    @Query("SELECT * FROM learning_records WHERE wordId = :wordId")
    suspend fun getLearningRecordsForWord(wordId: Long): List<LearningRecordEntity>

    @Query("SELECT * FROM learning_records WHERE wordId = :wordId")
    fun observeLearningRecord(wordId: Long): Flow<LearningRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyEvent(event: StudyEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailySummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklySummary(summary: WeeklySummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearnerProfile(profile: LearnerProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanHistory(plan: PlanHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfusionEdge(edge: ConfusionEdgeEntity)
}
