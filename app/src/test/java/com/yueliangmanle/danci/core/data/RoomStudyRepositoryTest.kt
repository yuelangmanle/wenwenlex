package com.yueliangmanle.danci.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.database.DanciDatabase
import com.yueliangmanle.danci.core.database.entity.LearnerProfileEntity
import com.yueliangmanle.danci.core.database.entity.PlanHistoryEntity
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomStudyRepositoryTest {

    private var database: DanciDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        database = null
    }

    @Test
    fun loadAiMemorySummary_readsExpandedPlanHistoryFields() = runTest {
        val db = buildTestDatabase().also { database = it }
        val repository = RoomStudyRepository(db.studyDao())
        db.studyDao().upsertLearnerProfile(
            LearnerProfileEntity(
                profileId = "default",
                vocabularyLevel = "提升中",
                checkpointSummariesJson = """
                    [
                      {
                        "checkpointId":"checkpoint-20260320-1",
                        "windowStartAt":"2026-03-20T09:00:00Z",
                        "windowEndAt":"2026-03-20T09:30:00Z",
                        "effectivePlanVersionId":7,
                        "candidatePlanVersionId":8,
                        "decisionStatus":"APPLIED",
                        "effectSummary":"正确率回升",
                        "signalSummary":"近义词误判升高"
                      }
                    ]
                """.trimIndent(),
                updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
            ),
        )
        db.studyDao().insertPlanHistory(
            PlanHistoryEntity(
                generatedAt = Instant.parse("2026-03-20T09:30:00Z"),
                summary = "先回拉错词，再恢复新词推进",
                recommendedFocus = listOf("abandon", "precise"),
                suggestedPace = "steady",
                suggestedModes = listOf("quiz", "word_detail"),
                severity = com.yueliangmanle.danci.core.model.PlanSeverity.MINOR,
                applyStatus = com.yueliangmanle.danci.core.model.PlanApplyStatus.APPLIED,
                executionEffect = "正确率回升",
            ),
        )

        val summary = repository.loadAiMemorySummary(planLimit = 10)

        assertEquals("APPLIED", summary.planHistory.single().applyStatus.name)
        assertEquals("manual_refresh", summary.planHistory.single().triggerType)
        assertEquals("LOCAL_FALLBACK", summary.planHistory.single().sourceType)
        assertTrue(summary.checkpointSummaries.isNotEmpty())
        assertEquals("checkpoint-20260320-1", summary.checkpointSummaries.single().checkpointId)
        assertEquals(
            com.yueliangmanle.danci.core.model.PlanApplyStatus.APPLIED,
            summary.checkpointSummaries.single().decisionStatus,
        )
        assertEquals("正确率回升", summary.checkpointSummaries.single().effectSummary)
        assertEquals("近义词误判升高", summary.checkpointSummaries.single().signalSummary)
    }

    @Test
    fun loadAiMemorySummary_skipsMalformedCheckpointEntries() = runTest {
        val db = buildTestDatabase().also { database = it }
        val repository = RoomStudyRepository(db.studyDao())
        db.studyDao().upsertLearnerProfile(
            LearnerProfileEntity(
                profileId = "default",
                checkpointSummariesJson = """
                    [
                      {
                        "checkpointId":"broken-entry",
                        "windowStartAt":"2026-03-20T08:00:00Z",
                        "windowEndAt":"2026-03-20T08:30:00Z",
                        "decisionStatus":"UNKNOWN",
                        "effectSummary":"这条不该被读到",
                        "signalSummary":"非法状态"
                      },
                      {
                        "checkpointId":"checkpoint-20260320-2",
                        "windowStartAt":"2026-03-20T09:00:00Z",
                        "windowEndAt":"2026-03-20T09:30:00Z",
                        "effectivePlanVersionId":9,
                        "candidatePlanVersionId":10,
                        "decisionStatus":"PENDING_CONFIRMATION",
                        "effectSummary":"等待确认",
                        "signalSummary":"学习疲劳升高"
                      }
                    ]
                """.trimIndent(),
                updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
            ),
        )

        val summary = repository.loadAiMemorySummary()

        assertEquals(1, summary.checkpointSummaries.size)
        assertEquals("checkpoint-20260320-2", summary.checkpointSummaries.single().checkpointId)
        assertEquals(
            com.yueliangmanle.danci.core.model.PlanApplyStatus.PENDING_CONFIRMATION,
            summary.checkpointSummaries.single().decisionStatus,
        )
    }

    private fun buildTestDatabase(): DanciDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DanciDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
}
