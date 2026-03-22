package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.AnalyticsOverview
import com.yueliangmanle.danci.core.model.BookProgressSnapshot
import com.yueliangmanle.danci.core.model.CheckpointSummary
import com.yueliangmanle.danci.core.model.DailyTrendPoint
import com.yueliangmanle.danci.core.model.FeedbackBucket
import com.yueliangmanle.danci.core.model.GoalProgressSnapshot
import com.yueliangmanle.danci.core.model.PHONETIC_SOURCE_AI
import com.yueliangmanle.danci.core.model.PHONETIC_STATUS_COMPLETE
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanEffectSnapshot
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.PlanSeverity
import com.yueliangmanle.danci.core.model.PronunciationUsageSnapshot
import com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRoundTripTest {
    @Test
    fun settingsJsonIncludesMultiProfileRoutingFields() {
        val settings = AppSettings(
            dailyGoal = 35,
            weeklyGoal = 80,
            phaseName = "六级冲刺",
            phaseTargetWords = 1500,
            activeBookId = "cet6",
            aiEnabled = true,
            defaultAiProfileId = "default-profile",
            wordHelpProfileId = "word-help",
            planAdjustmentProfileId = "plan-profile",
            phoneticFillProfileId = "phonetic-profile",
        )

        val json = settings.toJson()

        assertEquals("default-profile", json.getString("default_ai_profile_id"))
        assertEquals("word-help", json.getString("word_help_profile_id"))
        assertEquals("plan-profile", json.getString("plan_adjustment_profile_id"))
        assertEquals("phonetic-profile", json.getString("phonetic_fill_profile_id"))
        assertEquals(80, json.getInt("weekly_goal"))
        assertEquals("六级冲刺", json.getString("phase_name"))
        assertEquals(1500, json.getInt("phase_target_words"))
    }

    @Test
    fun wordJsonIncludesDualPhoneticFields() {
        val word = WordEntity(
            id = 1,
            lemma = "abandon",
            phonetic = "/əˈbændən/",
            phoneticUk = "/əˈbændən/",
            phoneticUs = "/əˈbændən/",
            phoneticSource = PHONETIC_SOURCE_AI,
            phoneticStatus = PHONETIC_STATUS_COMPLETE,
            phoneticUpdatedAt = Instant.parse("2026-03-19T10:00:00Z"),
            meanings = listOf("放弃"),
        )

        val json = word.toJson()

        assertEquals("/əˈbændən/", json.getString("phonetic_uk"))
        assertEquals("/əˈbændən/", json.getString("phonetic_us"))
        assertEquals(PHONETIC_SOURCE_AI, json.getString("phonetic_source"))
        assertEquals(PHONETIC_STATUS_COMPLETE, json.getString("phonetic_status"))
        assertTrue(json.has("phonetic_updated_at"))
    }

    @Test
    fun importerAcceptsLegacyVersionOneBackup() {
        val bytes = zipBackup(
            manifest = JSONObject()
                .put("version", 1)
                .put("created_at", "2026-03-19T10:00:00Z")
                .put("sections", org.json.JSONArray(REQUIRED_BACKUP_SECTIONS)),
            payload = JSONObject()
                .put(
                    "settings",
                    JSONObject()
                        .put("daily_goal", 20)
                        .put("active_book_id", JSONObject.NULL)
                        .put("ai_enabled", false)
                        .put("ai_base_url", "https://api.openai.com/v1")
                        .put("ai_model", "gpt-5-mini")
                        .put("ai_plan_adjustment_enabled", true)
                        .put("ai_session_checkpoint_enabled", true)
                        .put("reminder_enabled", false)
                        .put("reminder_hour", 21)
                        .put("reminder_minute", 0),
                )
                .put("books", org.json.JSONArray())
                .put("book_words", org.json.JSONArray())
                .put(
                    "words",
                    org.json.JSONArray().put(
                        JSONObject()
                            .put("id", 1)
                            .put("lemma", "abandon")
                            .put("phonetic", "/əˈbændən/")
                            .put("part_of_speech", org.json.JSONArray())
                            .put("meanings", org.json.JSONArray().put("放弃"))
                            .put("synonyms", org.json.JSONArray())
                            .put("antonyms", org.json.JSONArray())
                            .put("similar_words", org.json.JSONArray())
                            .put("confusing_words", org.json.JSONArray())
                            .put("word_forms", org.json.JSONArray())
                            .put("tags", org.json.JSONArray()),
                    ),
                )
                .put("learning_records", org.json.JSONArray())
                .put("study_sessions", org.json.JSONArray())
                .put("study_events", org.json.JSONArray())
                .put("ai_memory_summary", JSONObject()),
        )

        val imported = BackupImporter().import(bytes)

        assertEquals(1, imported.manifest.version)
        assertEquals("/əˈbændən/", imported.snapshot.words.single().phonetic)
    }

    @Test
    fun aiMemorySummary_roundTripsCheckpointSummariesAndExpandedPlanHistory() {
        val summary = AiMemorySummary(
            planHistory = listOf(
                PlanHistoryEntry(
                    id = 5L,
                    generatedAt = Instant.parse("2026-03-19T10:00:00Z"),
                    summary = "建议先回拉错词。",
                    parentPlanVersionId = 3L,
                    triggerType = "study_checkpoint",
                    sourceType = "AI",
                    recommendedFocus = listOf("abandon"),
                    suggestedModes = listOf("quiz", "dictation"),
                    suggestedPace = "slow_down",
                    reasonSummary = "最近近义词误判升高。",
                    changeSummary = "减少新词推进，增加复习。",
                    abnormalSignals = listOf("近义词误判升高"),
                    severity = PlanSeverity.MAJOR,
                    applyStatus = PlanApplyStatus.PENDING_CONFIRMATION,
                    executionEffect = "预计先压住错词率。",
                ),
            ),
            checkpointSummaries = listOf(
                CheckpointSummary(
                    checkpointId = "cp-1",
                    windowStartAt = Instant.parse("2026-03-19T09:00:00Z"),
                    windowEndAt = Instant.parse("2026-03-19T10:00:00Z"),
                    effectivePlanVersionId = 3L,
                    candidatePlanVersionId = 5L,
                    decisionStatus = PlanApplyStatus.PENDING_CONFIRMATION,
                    effectSummary = "先稳住复习正确率。",
                    signalSummary = "近义词误判升高",
                ),
            ),
            analyticsSnapshot = LearningAnalyticsSnapshot(
                overview = AnalyticsOverview(
                    accuracyRate = 0.82f,
                    studiedDays = 7,
                    masteredCount = 58,
                ),
                dailyTrend = listOf(
                    DailyTrendPoint(
                        date = "2026-03-19",
                        studiedCount = 18,
                        correctRate = 0.82f,
                    ),
                ),
                feedbackBreakdown = listOf(
                    FeedbackBucket(
                        label = "发音",
                        count = 2,
                        ratio = 0.1f,
                    ),
                ),
                bookProgress = listOf(
                    BookProgressSnapshot(
                        bookId = "cet6",
                        bookName = "六级核心词",
                        completedCount = 80,
                        totalCount = 200,
                    ),
                ),
                planEffects = listOf(
                    PlanEffectSnapshot(
                        planVersionId = 5L,
                        label = "先复习后推进",
                        beforeCorrectRate = 0.74f,
                        afterCorrectRate = 0.82f,
                        outcomeSummary = "正确率回升",
                    ),
                ),
                pronunciationUsage = PronunciationUsageSnapshot(
                    followReadCount = 6,
                    voicePlaybackCount = 12,
                    shadowingCount = 2,
                ),
            ),
            longTermInsights = listOf("发音巩固后带动正确率回升"),
            goalProgress = GoalProgressSnapshot(
                currentDayCompletedCount = 14,
                currentWeekCompletedCount = 58,
                currentStreakDays = 8,
                bestStreakDays = 13,
                phaseName = "六级冲刺",
                phaseTargetWords = 1500,
                phaseCompletedWords = 612,
            ),
            upgradeHealth = mapOf(
                "db_migration" to "ok",
                "backup_integrity" to "ok",
            ),
        )

        val json = summary.toJson()
        val restored = json.toAiMemorySummary()

        assertTrue(json.has("analytics_snapshot"))
        assertTrue(json.getJSONObject("analytics_snapshot").has("daily_trend"))
        assertTrue(json.getJSONObject("analytics_snapshot").has("feedback_breakdown"))
        assertTrue(json.getJSONObject("analytics_snapshot").has("book_progress"))
        assertTrue(json.getJSONObject("analytics_snapshot").has("plan_effects"))
        assertTrue(json.getJSONObject("analytics_snapshot").has("pronunciation_usage"))
        assertTrue(json.has("long_term_insights"))
        assertTrue(json.has("goal_progress"))
        assertTrue(json.has("upgrade_health"))
        assertEquals(1, restored.checkpointSummaries.size)
        assertEquals(PlanApplyStatus.PENDING_CONFIRMATION, restored.planHistory.single().applyStatus)
        assertEquals("cp-1", restored.checkpointSummaries.single().checkpointId)
        assertEquals("先复习后推进", restored.analyticsSnapshot.planEffects.single().label)
        assertEquals("发音巩固后带动正确率回升", restored.longTermInsights.single())
        assertEquals(58, restored.goalProgress.currentWeekCompletedCount)
        assertEquals("六级冲刺", restored.goalProgress.phaseName)
        assertEquals("ok", restored.upgradeHealth["db_migration"])
    }

    @Test
    fun importerAcceptsLegacyVersionFourBackupAndBackfillsAnalyticsDefaults() {
        val bytes = zipBackup(
            manifest = JSONObject()
                .put("version", 4)
                .put("created_at", "2026-03-19T10:00:00Z")
                .put("sections", org.json.JSONArray(REQUIRED_BACKUP_SECTIONS)),
            payload = JSONObject()
                .put(
                    "settings",
                    JSONObject()
                        .put("daily_goal", 20)
                        .put("active_book_id", JSONObject.NULL)
                        .put("ai_enabled", false)
                        .put("ai_base_url", "https://api.openai.com/v1")
                        .put("ai_model", "gpt-5-mini")
                        .put("ai_plan_adjustment_enabled", true)
                        .put("ai_session_checkpoint_enabled", true)
                        .put("reminder_enabled", false)
                        .put("reminder_hour", 21)
                        .put("reminder_minute", 0),
                )
                .put("books", org.json.JSONArray())
                .put("book_words", org.json.JSONArray())
                .put("words", org.json.JSONArray())
                .put("learning_records", org.json.JSONArray())
                .put("study_sessions", org.json.JSONArray())
                .put("study_events", org.json.JSONArray())
                .put(
                    "ai_memory_summary",
                    JSONObject()
                        .put("plan_history", org.json.JSONArray())
                        .put("checkpoint_summaries", org.json.JSONArray()),
                ),
        )

        val imported = BackupImporter().import(bytes)

        assertTrue(imported.snapshot.aiMemorySummary.analyticsSnapshot.planEffects.isEmpty())
        assertTrue(imported.snapshot.aiMemorySummary.longTermInsights.isEmpty())
    }

    @Test
    fun learningRecord_roundTripsExpandedSignals() {
        val entity = LearningRecordEntity(
            wordId = 1L,
            mastery = 0.7f,
            familiarityState = "学习中",
            forgettingRiskScore = 0.66f,
            reviewPriorityScore = 0.91f,
            proficiencyBand = "unstable",
            lastResponseLatencyMs = 4100L,
            averageResponseLatencyMs = 3750L,
            consecutiveMistakeCount = 3,
            lastMistakeAt = Instant.parse("2026-03-19T10:00:00Z"),
        )

        val restored = entity.toJson().toLearningRecordEntity()

        assertEquals(0.66f, restored.forgettingRiskScore, 0.0001f)
        assertEquals(0.91f, restored.reviewPriorityScore, 0.0001f)
        assertEquals("unstable", restored.proficiencyBand)
        assertEquals(4100L, restored.lastResponseLatencyMs)
        assertEquals(3750L, restored.averageResponseLatencyMs)
        assertEquals(3, restored.consecutiveMistakeCount)
        assertEquals(Instant.parse("2026-03-19T10:00:00Z"), restored.lastMistakeAt)
    }
}

private fun zipBackup(
    manifest: JSONObject,
    payload: JSONObject,
): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        zip.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
        zip.write(manifest.toString().toByteArray())
        zip.closeEntry()

        zip.putNextEntry(ZipEntry(PAYLOAD_FILE_NAME))
        zip.write(payload.toString().toByteArray())
        zip.closeEntry()
    }
    return output.toByteArray()
}
