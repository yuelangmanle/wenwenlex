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
                        "title":"阶段检查",
                        "suggestion":"先回拉错词再恢复新词",
                        "sourceLabel":"AI 生成",
                        "createdAt":"2026-03-20T09:30:00Z"
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
    }

    private fun buildTestDatabase(): DanciDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DanciDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
}
