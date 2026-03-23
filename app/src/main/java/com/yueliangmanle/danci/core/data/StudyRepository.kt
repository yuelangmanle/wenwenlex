package com.yueliangmanle.danci.core.data

import com.yueliangmanle.danci.core.database.dao.StudyDao
import com.yueliangmanle.danci.core.database.entity.ConfusionEdgeEntity
import com.yueliangmanle.danci.core.database.entity.DailySummaryEntity
import com.yueliangmanle.danci.core.database.entity.LearnerProfileEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.PlanHistoryEntity
import com.yueliangmanle.danci.core.database.entity.StudyEventEntity
import com.yueliangmanle.danci.core.database.entity.StudySessionEntity
import com.yueliangmanle.danci.core.database.entity.WeeklySummaryEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.ConfusionEdge
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudySession
import com.yueliangmanle.danci.core.model.WeeklySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StudyRepository {
    fun observeLearningRecord(wordId: Long): Flow<LearningRecord?>
    suspend fun getLearningRecordOrDefault(wordId: Long): LearningRecord
    suspend fun getLearningRecordsForWord(wordId: Long): List<LearningRecord>
    suspend fun getAllLearningRecords(): List<LearningRecord>
    suspend fun getAllStudySessions(): List<StudySession>
    suspend fun upsertLearningRecord(record: LearningRecord)
    suspend fun startSession(session: StudySession): Long
    suspend fun appendEvent(event: StudyEvent): Long
    suspend fun getStudyEventsSince(since: java.time.Instant): List<StudyEvent>
    suspend fun getAllStudyEvents(): List<StudyEvent>
    suspend fun getRecentStudyEvents(limit: Int = 200): List<StudyEvent>
    suspend fun loadAiMemorySummary(
        dailyLimit: Int = 7,
        weeklyLimit: Int = 4,
        planLimit: Int = 10,
        confusionLimit: Int = 20,
    ): AiMemorySummary
    suspend fun saveAiMemorySummary(summary: AiMemorySummary)
}

class RoomStudyRepository(
    private val studyDao: StudyDao,
) : StudyRepository {
    override fun observeLearningRecord(wordId: Long): Flow<LearningRecord?> =
        studyDao.observeLearningRecord(wordId).map { it?.asExternalModel() }

    override suspend fun getLearningRecordOrDefault(wordId: Long): LearningRecord =
        studyDao.getLearningRecordsForWord(wordId).firstOrNull()?.asExternalModel()
            ?: defaultLearningRecord(wordId)

    override suspend fun getLearningRecordsForWord(wordId: Long): List<LearningRecord> =
        studyDao.getLearningRecordsForWord(wordId).map(LearningRecordEntity::asExternalModel)

    override suspend fun getAllLearningRecords(): List<LearningRecord> =
        studyDao.getAllLearningRecords().map(LearningRecordEntity::asExternalModel)

    override suspend fun getAllStudySessions(): List<StudySession> =
        studyDao.getAllStudySessions().map(StudySessionEntity::asExternalModel)

    override suspend fun upsertLearningRecord(record: LearningRecord) {
        studyDao.upsertLearningRecord(record.asEntity())
    }

    override suspend fun startSession(session: StudySession): Long =
        studyDao.insertStudySession(session.asEntity())

    override suspend fun appendEvent(event: StudyEvent): Long =
        studyDao.insertStudyEvent(event.asEntity())

    override suspend fun getStudyEventsSince(since: java.time.Instant): List<StudyEvent> =
        studyDao.getStudyEventsSince(since).map(StudyEventEntity::asExternalModel)

    override suspend fun getAllStudyEvents(): List<StudyEvent> =
        studyDao.getAllStudyEvents().map(StudyEventEntity::asExternalModel)

    override suspend fun getRecentStudyEvents(limit: Int): List<StudyEvent> =
        studyDao.getRecentStudyEvents(limit).map(StudyEventEntity::asExternalModel)

    override suspend fun loadAiMemorySummary(
        dailyLimit: Int,
        weeklyLimit: Int,
        planLimit: Int,
        confusionLimit: Int,
    ): AiMemorySummary =
        AiMemorySummary(
            learnerProfile = studyDao.getLearnerProfile(LearnerProfile.DEFAULT_PROFILE_ID)?.asExternalModel(),
            dailySummaries = studyDao.getDailySummaries(dailyLimit)
                .map(DailySummaryEntity::asExternalModel)
                .sortedBy(DailySummary::date),
            weeklySummaries = studyDao.getWeeklySummaries(weeklyLimit)
                .map(WeeklySummaryEntity::asExternalModel)
                .sortedBy(WeeklySummary::weekStartDate),
            planHistory = studyDao.getPlanHistory(planLimit)
                .map(PlanHistoryEntity::asExternalModel)
                .sortedBy(PlanHistoryEntry::generatedAt),
            confusionEdges = studyDao.getConfusionEdges(confusionLimit).map(ConfusionEdgeEntity::asExternalModel),
        )

    override suspend fun saveAiMemorySummary(summary: AiMemorySummary) {
        summary.learnerProfile?.let { studyDao.upsertLearnerProfile(it.asEntity()) }
        summary.dailySummaries.forEach { studyDao.upsertDailySummary(it.asEntity()) }
        summary.weeklySummaries.forEach { studyDao.upsertWeeklySummary(it.asEntity()) }
        summary.planHistory.forEach { studyDao.insertPlanHistory(it.asEntity()) }
        summary.confusionEdges.forEach { studyDao.upsertConfusionEdge(it.asEntity()) }
    }
}

fun defaultLearningRecord(wordId: Long): LearningRecord =
    LearningRecord(
        wordId = wordId,
        mastery = 0.3f,
        familiarityState = "未学",
        learningStage = "UNSEEN",
    )

internal fun LearningRecordEntity.asExternalModel(): LearningRecord =
    LearningRecord(
        wordId = wordId,
        mastery = mastery,
        familiarityState = familiarityState,
        reviewStage = reviewStage,
        learningStage = learningStage,
        introducedAt = introducedAt,
        nextReviewAt = nextReviewAt,
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        consecutiveCorrectCount = consecutiveCorrectCount,
        lastReviewedAt = lastReviewedAt,
        lastOutcome = lastOutcome,
        lastMistakeAt = lastMistakeAt,
        lastFuzzyAt = lastFuzzyAt,
        lastStudyMode = lastStudyMode,
        currentGroupPassState = currentGroupPassState,
        confusionWeight = confusionWeight,
        similarSpellingWeight = similarSpellingWeight,
    )

internal fun LearningRecord.asEntity(): LearningRecordEntity =
    LearningRecordEntity(
        wordId = wordId,
        mastery = mastery,
        familiarityState = familiarityState,
        reviewStage = reviewStage,
        learningStage = learningStage,
        introducedAt = introducedAt,
        nextReviewAt = nextReviewAt,
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        consecutiveCorrectCount = consecutiveCorrectCount,
        lastReviewedAt = lastReviewedAt,
        lastOutcome = lastOutcome,
        lastMistakeAt = lastMistakeAt,
        lastFuzzyAt = lastFuzzyAt,
        lastStudyMode = lastStudyMode,
        currentGroupPassState = currentGroupPassState,
        confusionWeight = confusionWeight,
        similarSpellingWeight = similarSpellingWeight,
    )

internal fun StudySession.asEntity(): StudySessionEntity =
    StudySessionEntity(
        id = id,
        mode = mode,
        targetBookId = targetBookId,
        scopeType = scopeType,
        scopeRef = scopeRef,
        groupSize = groupSize,
        currentGroupIndex = currentGroupIndex,
        startedAt = startedAt,
        finishedAt = finishedAt,
        plannedCount = plannedCount,
        completedCount = completedCount,
        correctCount = correctCount,
        wrongCount = wrongCount,
        strategySnapshot = strategySnapshot,
    )

internal fun StudyEvent.asEntity(): StudyEventEntity =
    StudyEventEntity(
        id = id,
        sessionId = sessionId,
        wordId = wordId,
        eventType = eventType,
        feedback = feedback,
        isCorrect = isCorrect,
        happenedAt = happenedAt,
        elapsedMillis = elapsedMillis,
        metadata = metadata,
    )

internal fun StudyEventEntity.asExternalModel(): StudyEvent =
    StudyEvent(
        id = id,
        sessionId = sessionId,
        wordId = wordId,
        eventType = eventType,
        feedback = feedback,
        isCorrect = isCorrect,
        happenedAt = happenedAt,
        elapsedMillis = elapsedMillis,
        metadata = metadata,
    )

internal fun StudySessionEntity.asExternalModel(): StudySession =
    StudySession(
        id = id,
        mode = mode,
        targetBookId = targetBookId,
        scopeType = scopeType,
        scopeRef = scopeRef,
        groupSize = groupSize,
        currentGroupIndex = currentGroupIndex,
        startedAt = startedAt,
        finishedAt = finishedAt,
        plannedCount = plannedCount,
        completedCount = completedCount,
        correctCount = correctCount,
        wrongCount = wrongCount,
        strategySnapshot = strategySnapshot,
    )

internal fun LearnerProfile.asEntity(): LearnerProfileEntity =
    LearnerProfileEntity(
        profileId = profileId,
        vocabularyLevel = vocabularyLevel,
        weakSpots = weakSpots,
        preferredQuestionTypes = preferredQuestionTypes,
        commonMistakePatterns = commonMistakePatterns,
        updatedAt = updatedAt,
    )

internal fun LearnerProfileEntity.asExternalModel(): LearnerProfile =
    LearnerProfile(
        profileId = profileId,
        vocabularyLevel = vocabularyLevel,
        weakSpots = weakSpots,
        preferredQuestionTypes = preferredQuestionTypes,
        commonMistakePatterns = commonMistakePatterns,
        updatedAt = updatedAt,
    )

internal fun DailySummary.asEntity(): DailySummaryEntity =
    DailySummaryEntity(
        date = date,
        studiedCount = studiedCount,
        reviewCount = reviewCount,
        correctRate = correctRate,
        fatigueNote = fatigueNote,
        primaryMistakeReasons = primaryMistakeReasons,
        updatedAt = updatedAt,
    )

internal fun DailySummaryEntity.asExternalModel(): DailySummary =
    DailySummary(
        date = date,
        studiedCount = studiedCount,
        reviewCount = reviewCount,
        correctRate = correctRate,
        fatigueNote = fatigueNote,
        primaryMistakeReasons = primaryMistakeReasons,
        updatedAt = updatedAt,
    )

internal fun WeeklySummary.asEntity(): WeeklySummaryEntity =
    WeeklySummaryEntity(
        weekStartDate = weekStartDate,
        studiedCount = studiedCount,
        correctRate = correctRate,
        trendSummary = trendSummary,
        persistentWeakSpots = persistentWeakSpots,
        updatedAt = updatedAt,
    )

internal fun WeeklySummaryEntity.asExternalModel(): WeeklySummary =
    WeeklySummary(
        weekStartDate = weekStartDate,
        studiedCount = studiedCount,
        correctRate = correctRate,
        trendSummary = trendSummary,
        persistentWeakSpots = persistentWeakSpots,
        updatedAt = updatedAt,
    )

internal fun PlanHistoryEntry.asEntity(): PlanHistoryEntity =
    PlanHistoryEntity(
        id = id,
        generatedAt = generatedAt,
        summary = summary,
        recommendedFocus = recommendedFocus,
        suggestedPace = suggestedPace,
        executionEffect = executionEffect,
    )

internal fun PlanHistoryEntity.asExternalModel(): PlanHistoryEntry =
    PlanHistoryEntry(
        id = id,
        generatedAt = generatedAt,
        summary = summary,
        recommendedFocus = recommendedFocus,
        suggestedPace = suggestedPace,
        executionEffect = executionEffect,
    )

internal fun ConfusionEdge.asEntity(): ConfusionEdgeEntity =
    ConfusionEdgeEntity(
        sourceWordId = sourceWordId,
        targetWordId = targetWordId,
        relationType = relationType,
        weight = weight,
        mistakeCount = mistakeCount,
        updatedAt = updatedAt,
    )

internal fun ConfusionEdgeEntity.asExternalModel(): ConfusionEdge =
    ConfusionEdge(
        sourceWordId = sourceWordId,
        targetWordId = targetWordId,
        relationType = relationType,
        weight = weight,
        mistakeCount = mistakeCount,
        updatedAt = updatedAt,
    )
