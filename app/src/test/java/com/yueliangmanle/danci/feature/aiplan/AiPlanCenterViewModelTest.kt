package com.yueliangmanle.danci.feature.aiplan

import com.yueliangmanle.danci.core.ai.PlanHistoryRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudySession
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiPlanCenterViewModelTest {
    @Test
    fun loadUiState_hidesLatestEffectCardWithoutAppliedPlanEffects() = runTest {
        val studyRepository = FakeStudyRepository(
            summary = AiMemorySummary(
                planHistory = listOf(
                    PlanHistoryEntry(
                        id = 12L,
                        generatedAt = Instant.parse("2026-03-21T13:00:00Z"),
                        summary = "建议暂停新词两天，回拉错词和近义词辨析。",
                        applyStatus = PlanApplyStatus.PENDING_CONFIRMATION,
                    ),
                ),
            ),
        )

        val state = AiPlanCenterViewModel(
            studyRepository = studyRepository,
            planHistoryRepository = PlanHistoryRepository(studyRepository = studyRepository),
        ).loadUiState()

        assertNull(state.latestPlanEffectTitle)
        assertNull(state.latestPlanEffectSummary)
    }

    @Test
    fun loadUiState_showsLatestEffectCardWhenAppliedPlanEffectExists() = runTest {
        val studyRepository = FakeStudyRepository(
            summary = AiMemorySummary(
                planHistory = listOf(
                    PlanHistoryEntry(
                        id = 9L,
                        generatedAt = Instant.parse("2026-03-20T13:00:00Z"),
                        summary = "当前计划保持稳态推进。",
                        applyStatus = PlanApplyStatus.APPLIED,
                    ),
                ),
                analyticsSnapshot = com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot(
                    planEffects = listOf(
                        com.yueliangmanle.danci.core.model.PlanEffectSnapshot(
                            planVersionId = 9L,
                            label = "先回拉错词",
                            beforeCorrectRate = 0.65f,
                            afterCorrectRate = 0.73f,
                            outcomeSummary = "正确率回升",
                        ),
                    ),
                ),
            ),
        )

        val state = AiPlanCenterViewModel(
            studyRepository = studyRepository,
            planHistoryRepository = PlanHistoryRepository(studyRepository = studyRepository),
        ).loadUiState()

        assertEquals("最近调整效果", state.latestPlanEffectTitle)
        assertEquals("先回拉错词：正确率回升，调整前 65%，调整后 73%。", state.latestPlanEffectSummary)
    }
}

private class FakeStudyRepository(
    private val summary: AiMemorySummary,
) : StudyRepository {
    override fun observeLearningRecord(wordId: Long): Flow<LearningRecord?> = flowOf(null)

    override suspend fun getLearningRecordOrDefault(wordId: Long): LearningRecord = error("Not needed")

    override suspend fun getLearningRecordsForWord(wordId: Long): List<LearningRecord> = emptyList()

    override suspend fun getAllLearningRecords(): List<LearningRecord> = emptyList()

    override suspend fun getAllStudySessions(): List<StudySession> = emptyList()

    override suspend fun upsertLearningRecord(record: LearningRecord) = Unit

    override suspend fun startSession(session: StudySession): Long = error("Not needed")

    override suspend fun appendEvent(event: StudyEvent): Long = error("Not needed")

    override suspend fun getStudyEventsSince(since: Instant): List<StudyEvent> = emptyList()

    override suspend fun getAllStudyEvents(): List<StudyEvent> = emptyList()

    override suspend fun getRecentStudyEvents(limit: Int): List<StudyEvent> = emptyList()

    override suspend fun loadAiMemorySummary(
        dailyLimit: Int,
        weeklyLimit: Int,
        planLimit: Int,
        confusionLimit: Int,
    ): AiMemorySummary = summary

    override suspend fun saveAiMemorySummary(summary: AiMemorySummary) = Unit
}
