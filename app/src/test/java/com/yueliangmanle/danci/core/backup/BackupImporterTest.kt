package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.GoalProgressSnapshot
import com.yueliangmanle.danci.core.model.LearnerProfile
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupImporterTest {
    @Test
    fun importRestoresSnapshotFromZipArchive() = runTest {
        val archive = BackupExporter(
            snapshotProvider = {
                BackupSnapshot(
                    settings = AppSettings(
                        dailyGoal = 35,
                        weeklyGoal = 80,
                        phaseName = "六级冲刺",
                        phaseTargetWords = 1500,
                        activeBookId = "cet4",
                        aiEnabled = true,
                        aiBaseUrl = "https://api.openai.com/v1",
                        aiModel = "gpt-5-mini",
                        reminderEnabled = true,
                        reminderHour = 21,
                        reminderMinute = 30,
                    ),
                    books = listOf(
                        BookEntity(
                            id = "cet4",
                            title = "四级核心词",
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
                            synonyms = listOf("give up"),
                            wordForms = listOf("abandoned", "abandoning"),
                        ),
                    ),
                    learningRecords = listOf(
                        LearningRecordEntity(
                            wordId = 1L,
                            mastery = 0.68f,
                            familiarityState = "学习中",
                            forgettingRiskScore = 0.61f,
                            reviewPriorityScore = 0.83f,
                            proficiencyBand = "unstable",
                            lastResponseLatencyMs = 4200L,
                            averageResponseLatencyMs = 3800L,
                            consecutiveMistakeCount = 2,
                            lastMistakeAt = Instant.parse("2026-03-18T11:58:00Z"),
                        ),
                    ),
                    aiMemorySummary = AiMemorySummary(
                        learnerProfile = LearnerProfile(
                            vocabularyLevel = "提升中",
                            weakSpots = listOf("abandon"),
                            updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                        ),
                        dailySummaries = listOf(
                            DailySummary(
                                date = "2026-03-18",
                                studiedCount = 24,
                                reviewCount = 10,
                                correctRate = 0.75f,
                                updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                            ),
                        ),
                        goalProgress = GoalProgressSnapshot(
                            currentWeekCompletedCount = 42,
                            currentStreakDays = 7,
                        ),
                        upgradeHealth = mapOf("db_migration" to "ok"),
                    ),
                )
            },
            nowProvider = { Instant.parse("2026-03-18T12:30:00Z") },
        ).export()

        val imported = BackupImporter().import(archive.zippedBytes)

        assertEquals(35, imported.snapshot.settings.dailyGoal)
        assertEquals(80, imported.snapshot.settings.weeklyGoal)
        assertEquals("六级冲刺", imported.snapshot.settings.phaseName)
        assertEquals(1500, imported.snapshot.settings.phaseTargetWords)
        assertEquals("cet4", imported.snapshot.settings.activeBookId)
        assertEquals("abandon", imported.snapshot.words.single().lemma)
        assertEquals(0.61f, imported.snapshot.learningRecords.single().forgettingRiskScore, 0.0001f)
        assertEquals(2, imported.snapshot.learningRecords.single().consecutiveMistakeCount)
        assertEquals("提升中", imported.snapshot.aiMemorySummary.learnerProfile?.vocabularyLevel)
        assertEquals(1, imported.snapshot.aiMemorySummary.dailySummaries.size)
        assertEquals(42, imported.snapshot.aiMemorySummary.goalProgress.currentWeekCompletedCount)
        assertEquals("ok", imported.snapshot.aiMemorySummary.upgradeHealth["db_migration"])
    }

    @Test
    fun importRejectsArchiveMissingRequiredSections() {
        val invalidManifest = BackupManifest(
            version = BACKUP_VERSION,
            createdAt = "2026-03-18T12:30:00Z",
            sections = listOf("books", "words"),
        )
        val payload = BackupSnapshot(
            settings = AppSettings(),
        ).toJson().toString()

        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupImporter().import(
                zipEntries(
                    manifest = invalidManifest.toJson().toString(),
                    payload = payload,
                ),
            )
        }

        assertEquals("Missing required backup section: settings", error.message)
    }

    @Test
    fun importer_fillsDefaultsForLegacyPlanHistoryRows() {
        val manifest = BackupManifest(
            version = 3,
            createdAt = "2026-03-18T12:30:00Z",
            sections = REQUIRED_BACKUP_SECTIONS,
        )
        val payload = JSONObject()
            .put("settings", BackupSnapshot(settings = AppSettings()).toJson().getJSONObject("settings"))
            .put("books", JSONArray())
            .put("book_words", JSONArray())
            .put("words", JSONArray())
            .put("learning_records", JSONArray())
            .put("study_sessions", JSONArray())
            .put("study_events", JSONArray())
            .put(
                "ai_memory_summary",
                JSONObject()
                    .put("plan_history", JSONArray().put(
                        JSONObject()
                            .put("id", 11)
                            .put("generated_at", "2026-03-18T08:00:00Z")
                            .put("summary", "旧版本计划")
                            .put("recommended_focus", JSONArray().put("abandon"))
                            .put("suggested_pace", "steady"),
                    )),
            )

        val imported = BackupImporter().import(
            zipEntries(
                manifest = manifest.toJson().toString(),
                payload = payload.toString(),
            ),
        )

        val plan = imported.snapshot.aiMemorySummary.planHistory.single()
        assertEquals(com.yueliangmanle.danci.core.model.PlanApplyStatus.APPLIED, plan.applyStatus)
        assertEquals(com.yueliangmanle.danci.core.model.PlanSeverity.MINOR, plan.severity)
        assertEquals(com.yueliangmanle.danci.core.model.PlanHistoryEntry.DEFAULT_TRIGGER_TYPE, plan.triggerType)
    }

    @Test
    fun importer_fillsDefaultsForVersionFourAnalyticsFields() {
        val manifest = BackupManifest(
            version = 4,
            createdAt = "2026-03-18T12:30:00Z",
            sections = REQUIRED_BACKUP_SECTIONS,
        )
        val payload = JSONObject()
            .put("settings", BackupSnapshot(settings = AppSettings()).toJson().getJSONObject("settings"))
            .put("books", JSONArray())
            .put("book_words", JSONArray())
            .put("words", JSONArray())
            .put("learning_records", JSONArray())
            .put("study_sessions", JSONArray())
            .put("study_events", JSONArray())
            .put(
                "ai_memory_summary",
                JSONObject()
                    .put(
                        "daily_summaries",
                        JSONArray().put(
                            JSONObject()
                                .put("date", "2026-03-18")
                                .put("studied_count", 12)
                                .put("review_count", 5)
                                .put("correct_rate", 0.75)
                                .put("updated_at", "2026-03-18T08:00:00Z"),
                        ),
                    ),
            )

        val imported = BackupImporter().import(
            zipEntries(
                manifest = manifest.toJson().toString(),
                payload = payload.toString(),
            ),
        )

        assertTrue(imported.snapshot.aiMemorySummary.analyticsSnapshot.planEffects.isEmpty())
        assertTrue(imported.snapshot.aiMemorySummary.longTermInsights.isEmpty())
    }

    @Test
    fun importV5Backup_buildsDefaultGoalProgressWhenMissing() {
        val payload = JSONObject()
            .put("settings", BackupSnapshot(settings = AppSettings()).toJson().getJSONObject("settings"))
            .put("books", JSONArray())
            .put("book_words", JSONArray())
            .put("words", JSONArray())
            .put("learning_records", JSONArray())
            .put("study_sessions", JSONArray())
            .put("study_events", JSONArray())
            .put("ai_memory_summary", JSONObject())

        val imported = payload.toString().toBackupSnapshot(version = 5)

        assertEquals(0, imported.aiMemorySummary.goalProgress.currentWeekCompletedCount)
        assertEquals(0, imported.aiMemorySummary.goalProgress.currentStreakDays)
        assertTrue(imported.aiMemorySummary.upgradeHealth.isEmpty())
    }

    @Test
    fun importV6Backup_marksMissingAudioAssetsAsStale() {
        val restored = BackupImporter().import(loadBackup(version = 6))

        assertTrue(
            restored.snapshot.wordAudioAssets.all { asset ->
                asset.assetState == "stale" || asset.assetState == "ready"
            },
        )
        assertEquals("stale", restored.snapshot.wordAudioAssets.single().assetState)
    }

    @Test
    fun importV6Backup_keepsReadableAudioAssetsReady() {
        val readableFile = java.io.File.createTempFile("wenwenlex-v6-", ".wav").apply {
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
            deleteOnExit()
        }
        val restored = BackupImporter().import(
            loadBackup(
                version = 6,
                localPath = readableFile.absolutePath,
            ),
        )

        assertEquals("ready", restored.snapshot.wordAudioAssets.single().assetState)
    }

    @Test
    fun importV7Backup_marksMissingAudioAssetsAsStale() {
        val restored = BackupImporter().import(
            loadBackup(
                version = 7,
                includeAssetState = true,
                localPath = "/path/not-exists/v7.wav",
            ),
        )

        assertEquals("stale", restored.snapshot.wordAudioAssets.single().assetState)
    }

    @Test
    fun importV7Backup_marksInaccessibleAudioAssetsAsStale() {
        val restored = BackupImporter().import(
            loadBackup(
                version = 7,
                includeAssetState = true,
                localPath = null,
            ),
        )

        assertEquals("stale", restored.snapshot.wordAudioAssets.single().assetState)
    }

    @Test
    fun importV7Backup_keepsReadableAudioAssetsReady() {
        val readableFile = java.io.File.createTempFile("wenwenlex-v7-", ".wav").apply {
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
            deleteOnExit()
        }
        val restored = BackupImporter().import(
            loadBackup(
                version = 7,
                includeAssetState = true,
                localPath = readableFile.absolutePath,
            ),
        )

        assertEquals("ready", restored.snapshot.wordAudioAssets.single().assetState)
    }

    @Test
    fun importV7Backup_preservesFailedStatusWhenAssetStateMissing() {
        val restored = BackupImporter().import(
            loadBackup(
                version = 7,
                includeAssetState = false,
                localPath = null,
                status = "failed",
            ),
        )

        assertEquals("failed", restored.snapshot.wordAudioAssets.single().assetState)
    }

    private fun loadBackup(
        version: Int,
        includeAssetState: Boolean = false,
        localPath: String? = "/path/not-exists/on-restore.wav",
        status: String = "ready",
    ): ByteArray {
        require(version == 6 || version == 7) { "Only v6/v7 fixtures are defined in this test." }
        val manifest = BackupManifest(
            version = version,
            createdAt = "2026-03-18T12:30:00Z",
            sections = REQUIRED_BACKUP_SECTIONS,
        )
        val payload = JSONObject()
            .put("settings", BackupSnapshot(settings = AppSettings()).toJson().getJSONObject("settings"))
            .put("books", JSONArray())
            .put("book_words", JSONArray())
            .put(
                "words",
                JSONArray().put(
                    JSONObject()
                        .put("id", 1)
                        .put("lemma", "abandon")
                        .put("part_of_speech", JSONArray())
                        .put("meanings", JSONArray().put("放弃"))
                        .put("synonyms", JSONArray())
                        .put("antonyms", JSONArray())
                        .put("similar_words", JSONArray())
                        .put("confusing_words", JSONArray())
                        .put("word_forms", JSONArray())
                        .put("tags", JSONArray()),
                ),
            )
            .put(
                "word_audio_assets",
                JSONArray().put(
                    JSONObject().apply {
                        put("id", 7)
                        put("word_id", 1)
                        put("accent", "us")
                        put("source_type", "offline_native_generated")
                        if (localPath != null) {
                            put("local_path", localPath)
                        } else {
                            put("local_path", JSONObject.NULL)
                        }
                        put("status", status)
                        if (includeAssetState) {
                            put("asset_state", "ready")
                        }
                    }
                ),
            )
            .put("learning_records", JSONArray())
            .put("study_sessions", JSONArray())
            .put("study_events", JSONArray())
            .put("ai_memory_summary", JSONObject())
        return zipEntries(
            manifest = manifest.toJson().toString(),
            payload = payload.toString(),
        )
    }

    private fun zipEntries(
        manifest: String,
        payload: String,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(PAYLOAD_FILE_NAME))
            zip.write(payload.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
