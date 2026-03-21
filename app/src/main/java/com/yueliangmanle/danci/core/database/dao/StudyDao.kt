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

    @Query("SELECT * FROM learning_records ORDER BY wordId ASC")
    suspend fun getAllLearningRecords(): List<LearningRecordEntity>

    @Query("SELECT * FROM learning_records WHERE wordId = :wordId")
    fun observeLearningRecord(wordId: Long): Flow<LearningRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySessions(sessions: List<StudySessionEntity>)

    @Query("SELECT * FROM study_sessions ORDER BY startedAt ASC")
    suspend fun getAllStudySessions(): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyEvent(event: StudyEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyEvents(events: List<StudyEventEntity>)

    @Query("SELECT * FROM study_events WHERE happenedAt >= :since ORDER BY happenedAt ASC")
    suspend fun getStudyEventsSince(since: java.time.Instant): List<StudyEventEntity>

    @Query("SELECT * FROM study_events ORDER BY happenedAt ASC")
    suspend fun getAllStudyEvents(): List<StudyEventEntity>

    @Query("SELECT * FROM study_events ORDER BY happenedAt DESC LIMIT :limit")
    suspend fun getRecentStudyEvents(limit: Int): List<StudyEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT :limit")
    suspend fun getDailySummaries(limit: Int): List<DailySummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklySummary(summary: WeeklySummaryEntity)

    @Query("SELECT * FROM weekly_summaries ORDER BY weekStartDate DESC LIMIT :limit")
    suspend fun getWeeklySummaries(limit: Int): List<WeeklySummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearnerProfile(profile: LearnerProfileEntity)

    @Query("SELECT * FROM learner_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getLearnerProfile(profileId: String): LearnerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanHistory(plan: PlanHistoryEntity): Long

    @Query("SELECT * FROM plan_history ORDER BY generatedAt DESC, id DESC LIMIT :limit")
    suspend fun getPlanHistory(limit: Int): List<PlanHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfusionEdge(edge: ConfusionEdgeEntity)

    @Query("SELECT * FROM confusion_edges ORDER BY mistakeCount DESC, weight DESC LIMIT :limit")
    suspend fun getConfusionEdges(limit: Int): List<ConfusionEdgeEntity>

    @Query("DELETE FROM study_events")
    suspend fun clearStudyEvents()

    @Query("DELETE FROM study_sessions")
    suspend fun clearStudySessions()

    @Query("DELETE FROM learning_records")
    suspend fun clearLearningRecords()

    @Query("DELETE FROM daily_summaries")
    suspend fun clearDailySummaries()

    @Query("DELETE FROM weekly_summaries")
    suspend fun clearWeeklySummaries()

    @Query("DELETE FROM learner_profiles")
    suspend fun clearLearnerProfiles()

    @Query("DELETE FROM plan_history")
    suspend fun clearPlanHistory()

    @Query("DELETE FROM confusion_edges")
    suspend fun clearConfusionEdges()
}
