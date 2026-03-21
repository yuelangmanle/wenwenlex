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
import com.yueliangmanle.danci.core.model.CheckpointSummary
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
import org.json.JSONArray
import org.json.JSONObject

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
    ): AiMemorySummary {
        val learnerProfileEntity = studyDao.getLearnerProfile(LearnerProfile.DEFAULT_PROFILE_ID)
        return AiMemorySummary(
            learnerProfile = learnerProfileEntity?.asExternalModel(),
            dailySummaries = studyDao.getDailySummaries(dailyLimit)
                .map(DailySummaryEntity::asExternalModel)
                .sortedBy(DailySummary::date),
            weeklySummaries = studyDao.getWeeklySummaries(weeklyLimit)
                .map(WeeklySummaryEntity::asExternalModel)
                .sortedBy(WeeklySummary::weekStartDate),
            planHistory = studyDao.getPlanHistory(planLimit)
                .map(PlanHistoryEntity::asExternalModel)
                .sortedBy(PlanHistoryEntry::generatedAt),
            checkpointSummaries = learnerProfileEntity?.checkpointSummariesJson.toCheckpointSummaries(),
            confusionEdges = studyDao.getConfusionEdges(confusionLimit).map(ConfusionEdgeEntity::asExternalModel),
        )
    }

    override suspend fun saveAiMemorySummary(summary: AiMemorySummary) {
        summary.toLearnerProfileEntity()?.let { studyDao.upsertLearnerProfile(it) }
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
    )

internal fun LearningRecordEntity.asExternalModel(): LearningRecord =
    LearningRecord(
        wordId = wordId,
        mastery = mastery,
        familiarityState = familiarityState,
        nextReviewAt = nextReviewAt,
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        consecutiveCorrectCount = consecutiveCorrectCount,
        lastReviewedAt = lastReviewedAt,
        lastOutcome = lastOutcome,
        confusionWeight = confusionWeight,
        similarSpellingWeight = similarSpellingWeight,
    )

internal fun LearningRecord.asEntity(): LearningRecordEntity =
    LearningRecordEntity(
        wordId = wordId,
        mastery = mastery,
        familiarityState = familiarityState,
        nextReviewAt = nextReviewAt,
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        consecutiveCorrectCount = consecutiveCorrectCount,
        lastReviewedAt = lastReviewedAt,
        lastOutcome = lastOutcome,
        confusionWeight = confusionWeight,
        similarSpellingWeight = similarSpellingWeight,
    )

internal fun StudySession.asEntity(): StudySessionEntity =
    StudySessionEntity(
        id = id,
        mode = mode,
        targetBookId = targetBookId,
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

internal fun AiMemorySummary.toLearnerProfileEntity(): LearnerProfileEntity? {
    val profile = learnerProfile
    if (profile == null && checkpointSummaries.isEmpty()) {
        return null
    }
    return LearnerProfileEntity(
        profileId = profile?.profileId ?: LearnerProfile.DEFAULT_PROFILE_ID,
        vocabularyLevel = profile?.vocabularyLevel,
        weakSpots = profile?.weakSpots.orEmpty(),
        preferredQuestionTypes = profile?.preferredQuestionTypes.orEmpty(),
        commonMistakePatterns = profile?.commonMistakePatterns.orEmpty(),
        checkpointSummariesJson = checkpointSummaries.toJsonString(),
        updatedAt = profile?.updatedAt ?: checkpointSummaries.maxOfOrNull(CheckpointSummary::createdAt) ?: java.time.Instant.EPOCH,
    )
}

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
        parentPlanVersionId = parentPlanVersionId,
        triggerType = triggerType,
        sourceType = sourceType,
        recommendedFocus = recommendedFocus,
        suggestedModes = suggestedModes,
        suggestedPace = suggestedPace,
        reasonSummary = reasonSummary,
        changeSummary = changeSummary,
        abnormalSignals = abnormalSignals,
        severity = severity,
        applyStatus = applyStatus,
        isHighlightedAiChange = isHighlightedAiChange,
        executionEffect = executionEffect,
        confirmedAt = confirmedAt,
        rejectedAt = rejectedAt,
    )

internal fun PlanHistoryEntity.asExternalModel(): PlanHistoryEntry =
    PlanHistoryEntry(
        id = id,
        generatedAt = generatedAt,
        summary = summary,
        parentPlanVersionId = parentPlanVersionId,
        triggerType = triggerType,
        sourceType = sourceType,
        recommendedFocus = recommendedFocus,
        suggestedModes = suggestedModes,
        suggestedPace = suggestedPace,
        reasonSummary = reasonSummary,
        changeSummary = changeSummary,
        abnormalSignals = abnormalSignals,
        severity = severity,
        applyStatus = applyStatus,
        isHighlightedAiChange = isHighlightedAiChange,
        executionEffect = executionEffect,
        confirmedAt = confirmedAt,
        rejectedAt = rejectedAt,
    )

private fun String?.toCheckpointSummaries(): List<CheckpointSummary> =
    runCatching {
        JSONArray(this ?: "[]").let { jsonArray ->
            buildList(jsonArray.length()) {
                repeat(jsonArray.length()) { index ->
                    val item = jsonArray.optJSONObject(index) ?: return@repeat
                    val title = item.optString("title").takeIf(String::isNotBlank) ?: return@repeat
                    val suggestion = item.optString("suggestion").takeIf(String::isNotBlank) ?: return@repeat
                    add(
                        CheckpointSummary(
                            title = title,
                            suggestion = suggestion,
                            sourceLabel = item.optString("sourceLabel").takeIf(String::isNotBlank),
                            createdAt = item.optString("createdAt")
                                .takeIf(String::isNotBlank)
                                ?.let(java.time.Instant::parse)
                                ?: java.time.Instant.EPOCH,
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

private fun List<CheckpointSummary>.toJsonString(): String =
    JSONArray(
        map { summary ->
            JSONObject()
                .put("title", summary.title)
                .put("suggestion", summary.suggestion)
                .put("sourceLabel", summary.sourceLabel)
                .put("createdAt", summary.createdAt.toString())
        },
    ).toString()

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
