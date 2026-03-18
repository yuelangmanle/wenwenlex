package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.WeeklySummary
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupExporterTest {
    @Test
    fun backupIncludesAiMemoryButExcludesApiKey() = runTest {
        val backup = BackupExporter(
            snapshotProvider = {
                BackupSnapshot(
                    settings = AppSettings(
                        dailyGoal = 30,
                        aiEnabled = true,
                        aiBaseUrl = "https://api.openai.com/v1",
                        aiModel = "gpt-5-mini",
                    ),
                    books = listOf(
                        BookEntity(
                            id = "cet4",
                            title = "四级核心词",
                            description = "测试词书",
                            wordCount = 1,
                            createdAt = Instant.parse("2026-03-18T12:00:00Z"),
                            updatedAt = Instant.parse("2026-03-18T12:00:00Z"),
                        ),
                    ),
                    words = listOf(
                        WordEntity(
                            id = 1L,
                            lemma = "abandon",
                            meanings = listOf("放弃"),
                        ),
                    ),
                    aiMemorySummary = sampleAiMemory(),
                )
            },
            nowProvider = { Instant.parse("2026-03-18T12:30:00Z") },
        ).export()

        assertTrue(backup.manifest.sections.contains("learner_profile"))
        assertTrue(backup.manifest.sections.contains("daily_summary"))
        assertTrue(backup.manifest.sections.contains("weekly_summary"))
        assertFalse(backup.serializedJson.contains("sk-secret"))
    }

    private fun sampleAiMemory(): AiMemorySummary =
        AiMemorySummary(
            learnerProfile = LearnerProfile(
                vocabularyLevel = "提升中",
                weakSpots = listOf("abandon"),
                preferredQuestionTypes = listOf("quiz"),
                commonMistakePatterns = listOf("similar_spelling"),
                updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
            ),
            dailySummaries = listOf(
                DailySummary(
                    date = "2026-03-18",
                    studiedCount = 25,
                    reviewCount = 12,
                    correctRate = 0.76f,
                    fatigueNote = "后段变慢",
                    primaryMistakeReasons = listOf("近义词辨析"),
                    updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                ),
            ),
            weeklySummaries = listOf(
                WeeklySummary(
                    weekStartDate = "2026-03-16",
                    studiedCount = 138,
                    correctRate = 0.78f,
                    trendSummary = "本周推进稳定",
                    persistentWeakSpots = listOf("拼写相近词"),
                    updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                ),
            ),
        )
}
