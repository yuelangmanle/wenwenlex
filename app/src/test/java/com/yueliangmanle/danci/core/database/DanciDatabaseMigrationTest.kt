package com.yueliangmanle.danci.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DanciDatabaseMigrationTest {

    private var helper: SupportSQLiteOpenHelper? = null
    private var databaseFile: File? = null

    @After
    fun tearDown() {
        helper?.close()
        helper = null
        databaseFile?.delete()
        databaseFile = null
    }

    @Test
    fun migrate3To4_addsReviewAndSessionScopeColumns() {
        val db = createVersion3Database()
        createVersion3Schema(db)
        db.execSQL("INSERT INTO words(id) VALUES (1)")
        db.execSQL("INSERT INTO words(id) VALUES (2)")
        db.execSQL(
            """
            INSERT INTO learning_records(
                wordId, mastery, familiarityState, nextReviewAt, reviewCount, lapseCount,
                consecutiveCorrectCount, lastReviewedAt, lastOutcome, confusionWeight, similarSpellingWeight
            ) VALUES (1, 0.4, 'LEARNING', NULL, 1, 0, 0, 1711152000000, 'known', 0, 0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO study_sessions(
                id, mode, targetBookId, startedAt, finishedAt, plannedCount, completedCount,
                correctCount, wrongCount, strategySnapshot
            ) VALUES (1, 'review', 'cet4', 1711152000000, NULL, 20, 0, 0, 0, NULL)
            """.trimIndent(),
        )

        MIGRATION_3_4.migrate(db)

        assertHasColumn(db, "learning_records", "learningStage")
        assertHasColumn(db, "learning_records", "reviewStage")
        assertHasColumn(db, "learning_records", "introducedAt")
        assertHasColumn(db, "learning_records", "lastMistakeAt")
        assertHasColumn(db, "learning_records", "lastFuzzyAt")
        assertHasColumn(db, "learning_records", "lastStudyMode")
        assertHasColumn(db, "learning_records", "currentGroupPassState")
        assertHasColumn(db, "study_sessions", "scopeType")
        assertHasColumn(db, "study_sessions", "scopeRef")
        assertHasColumn(db, "study_sessions", "groupSize")
        assertHasColumn(db, "study_sessions", "currentGroupIndex")

        db.query(
            """
            SELECT learningStage, reviewStage, scopeType, groupSize, currentGroupIndex
            FROM learning_records
            JOIN study_sessions ON study_sessions.id = 1
            WHERE wordId = 1
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("UNSEEN", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals("book", cursor.getString(2))
            assertEquals(5, cursor.getInt(3))
            assertEquals(0, cursor.getInt(4))
        }

        db.execSQL(
            """
            INSERT INTO learning_records(
                wordId, mastery, familiarityState, nextReviewAt, reviewCount, lapseCount,
                consecutiveCorrectCount, lastReviewedAt, lastOutcome, confusionWeight,
                similarSpellingWeight, learningStage, reviewStage, introducedAt,
                lastMistakeAt, lastFuzzyAt, lastStudyMode, currentGroupPassState
            ) VALUES (
                2, 0.8, '熟悉', 1711238400000, 4, 1, 2, 1711152000000, 'known', 0, 0,
                'FAMILIAR', 4, 1711065600000, 1710979200000, 1710892800000, 'review', 'passed'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO study_sessions(
                id, mode, targetBookId, startedAt, finishedAt, plannedCount, completedCount,
                correctCount, wrongCount, strategySnapshot, scopeType, scopeRef, groupSize, currentGroupIndex
            ) VALUES (
                2, 'review', 'cet4', 1711152000000, NULL, 20, 10, 8, 2, NULL,
                'active_book', 'cet4', 10, 1
            )
            """.trimIndent(),
        )

        db.query(
            """
            SELECT learningStage, reviewStage, lastStudyMode, currentGroupPassState
            FROM learning_records
            WHERE wordId = 2
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("FAMILIAR", cursor.getString(0))
            assertEquals(4, cursor.getInt(1))
            assertEquals("review", cursor.getString(2))
            assertEquals("passed", cursor.getString(3))
        }

        db.query(
            """
            SELECT scopeType, scopeRef, groupSize, currentGroupIndex
            FROM study_sessions
            WHERE id = 2
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("active_book", cursor.getString(0))
            assertEquals("cet4", cursor.getString(1))
            assertEquals(10, cursor.getInt(2))
            assertEquals(1, cursor.getInt(3))
        }
    }

    @Test
    fun migrate4To5_createsAudioGenerationJobsTable() {
        val db = createVersion4Database()
        createVersion4Schema(db)

        MIGRATION_4_5.migrate(db)

        assertHasColumn(db, "audio_generation_jobs", "jobType")
        assertHasColumn(db, "audio_generation_jobs", "sourceType")
        assertHasColumn(db, "audio_generation_jobs", "scopeType")
        assertHasColumn(db, "audio_generation_jobs", "scopeRef")
        assertHasColumn(db, "audio_generation_jobs", "status")
        assertHasColumn(db, "audio_generation_jobs", "totalCount")
        assertHasColumn(db, "audio_generation_jobs", "completedCount")
        assertHasColumn(db, "audio_generation_jobs", "failedCount")
        assertHasColumn(db, "audio_generation_jobs", "createdAt")
        assertHasColumn(db, "audio_generation_jobs", "updatedAt")
        assertHasColumn(db, "audio_generation_jobs", "lastError")

        db.execSQL(
            """
            INSERT INTO audio_generation_jobs(
                jobType, sourceType, scopeType, scopeRef, status,
                totalCount, completedCount, failedCount, createdAt, updatedAt, lastError
            ) VALUES (
                'dictionary_prefetch', 'dictionary_cache', 'active_book', 'cet4', 'queued',
                20, 3, 1, 1711267200000, 1711267200000, NULL
            )
            """.trimIndent(),
        )

        db.query(
            """
            SELECT jobType, sourceType, scopeType, scopeRef, totalCount, completedCount, failedCount
            FROM audio_generation_jobs
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("dictionary_prefetch", cursor.getString(0))
            assertEquals("dictionary_cache", cursor.getString(1))
            assertEquals("active_book", cursor.getString(2))
            assertEquals("cet4", cursor.getString(3))
            assertEquals(20, cursor.getInt(4))
            assertEquals(3, cursor.getInt(5))
            assertEquals(1, cursor.getInt(6))
        }
    }

    @Test
    fun migrateLegacyVersion8To9_rebuildsCurrentSchemaAndPreservesCoreData() {
        val db = createVersion8Database()
        createLegacyVersion8Schema(db)
        db.execSQL("INSERT INTO words(id) VALUES (1)")
        db.execSQL(
            """
            INSERT INTO learning_records(
                wordId, mastery, familiarityState, nextReviewAt, reviewCount, lapseCount,
                consecutiveCorrectCount, lastReviewedAt, lastOutcome, confusionWeight,
                similarSpellingWeight, forgettingRiskScore, reviewPriorityScore,
                proficiencyBand, lastResponseLatencyMs, averageResponseLatencyMs,
                consecutiveMistakeCount, lastMistakeAt
            ) VALUES (
                1, 0.65, 'reviewing', 1711411200000, 5, 1,
                2, 1711324800000, 'hard', 0.3, 0.1, 0.6, 0.7,
                'familiar', 1800, 2200, 1, 1711238400000
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO study_sessions(
                id, mode, targetBookId, startedAt, finishedAt, plannedCount, completedCount,
                correctCount, wrongCount, strategySnapshot
            ) VALUES (
                11, 'review', 'cet4', 1711324800000, NULL, 20, 5, 4, 1, '{"pace":"normal"}'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO learner_profiles(
                profileId, vocabularyLevel, weakSpots, preferredQuestionTypes,
                commonMistakePatterns, checkpointSummariesJson, analyticsSnapshotJson,
                longTermInsightsJson, goalProgressJson, upgradeHealthJson, updatedAt
            ) VALUES (
                'default', 'CET4', '拼写词义', '选择题', '词义混淆',
                '[]', '{"retention":0.8}', '["持续复习"]', '{"dailyGoal":20}',
                '{"status":"ok"}', 1711324800000
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO plan_history(
                id, generatedAt, summary, parentPlanVersionId, triggerType, sourceType,
                recommendedFocus, suggestedModes, suggestedPace, reasonSummary,
                changeSummary, abnormalSignals, severity, applyStatus,
                isHighlightedAiChange, executionEffect, confirmedAt, rejectedAt
            ) VALUES (
                7, 1711324800000, '加强拼写复习', NULL, 'manual_refresh', 'LOCAL_FALLBACK',
                '拼写听辨', 'reviewdictation', 'slow', '错词增多',
                '增加复习', '["mistakes"]', 'MINOR', 'APPLIED',
                0, '执行良好', NULL, NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO word_audio_assets(
                id, wordId, accent, sourceType, remoteUrl, localPath, mimeType,
                checksum, status, fetchedAt, lastPlayedAt, lastError, failureCount,
                sourceId, presetId, actualSourceType, namespace, assetState, taskId
            ) VALUES (
                9, 1, 'us', 'cloud_tts', 'https://example.com/a.mp3', '/tmp/a.mp3',
                'audio/mpeg', 'abc', 'ready', 1711324800000, 1711328400000, NULL, 0,
                'mimo-default', 'warm', 'cloud_tts', 'word', 'ready', 'task-1'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO voice_packs(
                id, name, locale, accent, engineType, version, downloadUrl, manifestUrl,
                checksumsUrl, installDir, archiveChecksum, installedSizeBytes, status,
                isActive, createdAt, updatedAt
            ) VALUES (
                'pack-us', '美式离线包', 'en-US', 'us', 'offline_native', '1.0',
                'https://example.com/us.zip', 'https://example.com/us.json',
                'https://example.com/us.sha', '/packs/us', 'sha256', 1024, 'installed',
                1, 1711324800000, 1711324800000
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO import_batches(
                id, bookId, fileName, sheetName, diagnosisSnapshotJson, parserMode,
                totalRows, importedRows, skippedRows, aiNormalizedCount,
                aiCompletedCount, createdAt
            ) VALUES (
                3, 'cet4', 'cet4.xlsx', 'Sheet1', '{"header":true}', 'ai_assisted',
                100, 95, 5, 12, 18, 1711324800000
            )
            """.trimIndent(),
        )

        MIGRATION_8_9.migrate(db)

        assertHasColumn(db, "learning_records", "reviewStage")
        assertHasColumn(db, "study_sessions", "scopeType")
        assertHasColumn(db, "audio_generation_jobs", "jobType")
        assertMissingColumn(db, "voice_packs", "checksumsUrl")
        assertMissingColumn(db, "word_audio_assets", "sourceId")
        assertMissingColumn(db, "plan_history", "triggerType")

        db.query(
            """
            SELECT reviewStage, learningStage, introducedAt, lastMistakeAt, currentGroupPassState
            FROM learning_records
            WHERE wordId = 1
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
            assertEquals("UNSEEN", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertEquals(1711238400000L, cursor.getLong(3))
            assertTrue(cursor.isNull(4))
        }

        db.query(
            """
            SELECT scopeType, scopeRef, groupSize, currentGroupIndex
            FROM study_sessions
            WHERE id = 11
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("book", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertEquals(5, cursor.getInt(2))
            assertEquals(0, cursor.getInt(3))
        }

        db.query(
            """
            SELECT vocabularyLevel, weakSpots, preferredQuestionTypes, commonMistakePatterns
            FROM learner_profiles
            WHERE profileId = 'default'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("CET4", cursor.getString(0))
            assertEquals("拼写词义", cursor.getString(1))
            assertEquals("选择题", cursor.getString(2))
            assertEquals("词义混淆", cursor.getString(3))
        }

        db.query(
            """
            SELECT summary, recommendedFocus, suggestedPace, executionEffect
            FROM plan_history
            WHERE id = 7
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("加强拼写复习", cursor.getString(0))
            assertEquals("拼写听辨", cursor.getString(1))
            assertEquals("slow", cursor.getString(2))
            assertEquals("执行良好", cursor.getString(3))
        }

        db.query(
            """
            SELECT remoteUrl, localPath, checksum, status, failureCount
            FROM word_audio_assets
            WHERE id = 9
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("https://example.com/a.mp3", cursor.getString(0))
            assertEquals("/tmp/a.mp3", cursor.getString(1))
            assertEquals("abc", cursor.getString(2))
            assertEquals("ready", cursor.getString(3))
            assertEquals(0, cursor.getInt(4))
        }

        db.query(
            """
            SELECT manifestUrl, installDir, archiveChecksum, installedSizeBytes, status, isActive
            FROM voice_packs
            WHERE id = 'pack-us'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("https://example.com/us.json", cursor.getString(0))
            assertEquals("/packs/us", cursor.getString(1))
            assertEquals("sha256", cursor.getString(2))
            assertEquals(1024L, cursor.getLong(3))
            assertEquals("installed", cursor.getString(4))
            assertEquals(1, cursor.getInt(5))
        }

        db.query(
            """
            SELECT fileName, sheetName, parserMode, totalRows, importedRows
            FROM import_batches
            WHERE id = 3
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("cet4.xlsx", cursor.getString(0))
            assertEquals("Sheet1", cursor.getString(1))
            assertEquals("ai_assisted", cursor.getString(2))
            assertEquals(100, cursor.getInt(3))
            assertEquals(95, cursor.getInt(4))
        }
    }

    @Test
    fun migrateCurrentVersion5To9_keepsCurrentUserDataAvailable() {
        val db = createVersion5Database()
        createCurrentVersion5Schema(db)
        db.execSQL("INSERT INTO words(id) VALUES (1)")
        db.execSQL(
            """
            INSERT INTO learning_records(
                wordId, mastery, familiarityState, reviewStage, learningStage, introducedAt,
                nextReviewAt, reviewCount, lapseCount, consecutiveCorrectCount,
                lastReviewedAt, lastOutcome, lastMistakeAt, lastFuzzyAt, lastStudyMode,
                currentGroupPassState, confusionWeight, similarSpellingWeight
            ) VALUES (
                1, 0.4, 'LEARNING', 2, 'IN_PROGRESS', 1711152000000,
                1711411200000, 3, 1, 2, 1711324800000, 'hard', 1711238400000,
                1711238400000, 'review', 'retry', 0.2, 0.1
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO study_sessions(
                id, mode, targetBookId, scopeType, scopeRef, groupSize, currentGroupIndex,
                startedAt, finishedAt, plannedCount, completedCount, correctCount, wrongCount,
                strategySnapshot
            ) VALUES (
                5, 'review', 'cet4', 'mistakes', 'recent', 8, 1,
                1711324800000, NULL, 15, 4, 3, 1, '{"mode":"mistakes"}'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO audio_generation_jobs(
                id, jobType, sourceType, scopeType, scopeRef, status,
                totalCount, completedCount, failedCount, createdAt, updatedAt, lastError
            ) VALUES (
                6, 'cloud_tts_prefetch', 'cloud_tts', 'book', 'cet4', 'running',
                30, 12, 1, 1711324800000, 1711328400000, NULL
            )
            """.trimIndent(),
        )

        MIGRATION_5_9.migrate(db)

        db.query(
            """
            SELECT reviewStage, learningStage, introducedAt, currentGroupPassState
            FROM learning_records
            WHERE wordId = 1
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
            assertEquals("IN_PROGRESS", cursor.getString(1))
            assertEquals(1711152000000L, cursor.getLong(2))
            assertEquals("retry", cursor.getString(3))
        }

        db.query(
            """
            SELECT scopeType, scopeRef, groupSize, currentGroupIndex
            FROM study_sessions
            WHERE id = 5
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("mistakes", cursor.getString(0))
            assertEquals("recent", cursor.getString(1))
            assertEquals(8, cursor.getInt(2))
            assertEquals(1, cursor.getInt(3))
        }

        db.query(
            """
            SELECT jobType, sourceType, status, totalCount, completedCount, failedCount
            FROM audio_generation_jobs
            WHERE id = 6
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("cloud_tts_prefetch", cursor.getString(0))
            assertEquals("cloud_tts", cursor.getString(1))
            assertEquals("running", cursor.getString(2))
            assertEquals(30, cursor.getInt(3))
            assertEquals(12, cursor.getInt(4))
            assertEquals(1, cursor.getInt(5))
        }
    }

    private fun createVersion3Database(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseFile = File(context.cacheDir, "migration-test-${System.nanoTime()}.db")
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseFile!!.absolutePath)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        return helper!!.writableDatabase
    }

    private fun createVersion4Database(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseFile = File(context.cacheDir, "migration-test-${System.nanoTime()}.db")
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseFile!!.absolutePath)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        return helper!!.writableDatabase
    }

    private fun createVersion5Database(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseFile = File(context.cacheDir, "migration-test-${System.nanoTime()}.db")
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseFile!!.absolutePath)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        return helper!!.writableDatabase
    }

    private fun createVersion8Database(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseFile = File(context.cacheDir, "migration-test-${System.nanoTime()}.db")
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseFile!!.absolutePath)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        return helper!!.writableDatabase
    }

    private fun createVersion3Schema(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS learning_records")
        db.execSQL("DROP TABLE IF EXISTS study_sessions")
        db.execSQL("DROP TABLE IF EXISTS words")
        db.execSQL("CREATE TABLE IF NOT EXISTS words (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_records (
                wordId INTEGER NOT NULL,
                mastery REAL NOT NULL,
                familiarityState TEXT NOT NULL,
                nextReviewAt INTEGER,
                reviewCount INTEGER NOT NULL DEFAULT 0,
                lapseCount INTEGER NOT NULL DEFAULT 0,
                consecutiveCorrectCount INTEGER NOT NULL DEFAULT 0,
                lastReviewedAt INTEGER,
                lastOutcome TEXT,
                confusionWeight REAL NOT NULL DEFAULT 0,
                similarSpellingWeight REAL NOT NULL DEFAULT 0,
                PRIMARY KEY(wordId),
                FOREIGN KEY(wordId) REFERENCES words(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_records_nextReviewAt ON learning_records(nextReviewAt)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mode TEXT NOT NULL,
                targetBookId TEXT,
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                plannedCount INTEGER NOT NULL DEFAULT 0,
                completedCount INTEGER NOT NULL DEFAULT 0,
                correctCount INTEGER NOT NULL DEFAULT 0,
                wrongCount INTEGER NOT NULL DEFAULT 0,
                strategySnapshot TEXT
            )
            """.trimIndent(),
        )
    }

    private fun createVersion4Schema(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS learning_records")
        db.execSQL("DROP TABLE IF EXISTS study_sessions")
        db.execSQL("DROP TABLE IF EXISTS words")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS words (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_records (
                wordId INTEGER NOT NULL,
                mastery REAL NOT NULL,
                familiarityState TEXT NOT NULL,
                reviewStage INTEGER NOT NULL DEFAULT 0,
                learningStage TEXT NOT NULL DEFAULT 'UNSEEN',
                introducedAt INTEGER,
                nextReviewAt INTEGER,
                reviewCount INTEGER NOT NULL DEFAULT 0,
                lapseCount INTEGER NOT NULL DEFAULT 0,
                consecutiveCorrectCount INTEGER NOT NULL DEFAULT 0,
                lastReviewedAt INTEGER,
                lastOutcome TEXT,
                lastMistakeAt INTEGER,
                lastFuzzyAt INTEGER,
                lastStudyMode TEXT,
                currentGroupPassState TEXT,
                confusionWeight REAL NOT NULL DEFAULT 0,
                similarSpellingWeight REAL NOT NULL DEFAULT 0,
                PRIMARY KEY(wordId),
                FOREIGN KEY(wordId) REFERENCES words(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mode TEXT NOT NULL,
                targetBookId TEXT,
                scopeType TEXT NOT NULL DEFAULT 'book',
                scopeRef TEXT,
                groupSize INTEGER NOT NULL DEFAULT 5,
                currentGroupIndex INTEGER NOT NULL DEFAULT 0,
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                plannedCount INTEGER NOT NULL DEFAULT 0,
                completedCount INTEGER NOT NULL DEFAULT 0,
                correctCount INTEGER NOT NULL DEFAULT 0,
                wrongCount INTEGER NOT NULL DEFAULT 0,
                strategySnapshot TEXT
            )
            """.trimIndent(),
        )
    }

    private fun createCurrentVersion5Schema(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS audio_generation_jobs")
        db.execSQL("DROP TABLE IF EXISTS learning_records")
        db.execSQL("DROP TABLE IF EXISTS study_sessions")
        db.execSQL("DROP TABLE IF EXISTS words")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS words (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_records (
                wordId INTEGER NOT NULL,
                mastery REAL NOT NULL,
                familiarityState TEXT NOT NULL,
                reviewStage INTEGER NOT NULL DEFAULT 0,
                learningStage TEXT NOT NULL DEFAULT 'UNSEEN',
                introducedAt INTEGER,
                nextReviewAt INTEGER,
                reviewCount INTEGER NOT NULL DEFAULT 0,
                lapseCount INTEGER NOT NULL DEFAULT 0,
                consecutiveCorrectCount INTEGER NOT NULL DEFAULT 0,
                lastReviewedAt INTEGER,
                lastOutcome TEXT,
                lastMistakeAt INTEGER,
                lastFuzzyAt INTEGER,
                lastStudyMode TEXT,
                currentGroupPassState TEXT,
                confusionWeight REAL NOT NULL DEFAULT 0,
                similarSpellingWeight REAL NOT NULL DEFAULT 0,
                PRIMARY KEY(wordId),
                FOREIGN KEY(wordId) REFERENCES words(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_records_nextReviewAt ON learning_records(nextReviewAt)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mode TEXT NOT NULL,
                targetBookId TEXT,
                scopeType TEXT NOT NULL DEFAULT 'book',
                scopeRef TEXT,
                groupSize INTEGER NOT NULL DEFAULT 5,
                currentGroupIndex INTEGER NOT NULL DEFAULT 0,
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                plannedCount INTEGER NOT NULL DEFAULT 0,
                completedCount INTEGER NOT NULL DEFAULT 0,
                correctCount INTEGER NOT NULL DEFAULT 0,
                wrongCount INTEGER NOT NULL DEFAULT 0,
                strategySnapshot TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS audio_generation_jobs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                jobType TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                scopeType TEXT NOT NULL,
                scopeRef TEXT NOT NULL,
                status TEXT NOT NULL,
                totalCount INTEGER NOT NULL DEFAULT 0,
                completedCount INTEGER NOT NULL DEFAULT 0,
                failedCount INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastError TEXT
            )
            """.trimIndent(),
        )
    }

    private fun createLegacyVersion8Schema(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS import_batches")
        db.execSQL("DROP TABLE IF EXISTS voice_packs")
        db.execSQL("DROP TABLE IF EXISTS word_audio_assets")
        db.execSQL("DROP TABLE IF EXISTS plan_history")
        db.execSQL("DROP TABLE IF EXISTS learner_profiles")
        db.execSQL("DROP TABLE IF EXISTS study_sessions")
        db.execSQL("DROP TABLE IF EXISTS learning_records")
        db.execSQL("DROP TABLE IF EXISTS words")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS words (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_records (
                wordId INTEGER NOT NULL,
                mastery REAL NOT NULL,
                familiarityState TEXT NOT NULL,
                nextReviewAt INTEGER,
                reviewCount INTEGER NOT NULL DEFAULT 0,
                lapseCount INTEGER NOT NULL DEFAULT 0,
                consecutiveCorrectCount INTEGER NOT NULL DEFAULT 0,
                lastReviewedAt INTEGER,
                lastOutcome TEXT,
                confusionWeight REAL NOT NULL DEFAULT 0,
                similarSpellingWeight REAL NOT NULL DEFAULT 0,
                forgettingRiskScore REAL NOT NULL DEFAULT 0,
                reviewPriorityScore REAL NOT NULL DEFAULT 0,
                proficiencyBand TEXT NOT NULL DEFAULT 'new',
                lastResponseLatencyMs INTEGER,
                averageResponseLatencyMs INTEGER,
                consecutiveMistakeCount INTEGER NOT NULL DEFAULT 0,
                lastMistakeAt INTEGER,
                PRIMARY KEY(wordId),
                FOREIGN KEY(wordId) REFERENCES words(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_records_nextReviewAt ON learning_records(nextReviewAt)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mode TEXT NOT NULL,
                targetBookId TEXT,
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                plannedCount INTEGER NOT NULL DEFAULT 0,
                completedCount INTEGER NOT NULL DEFAULT 0,
                correctCount INTEGER NOT NULL DEFAULT 0,
                wrongCount INTEGER NOT NULL DEFAULT 0,
                strategySnapshot TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learner_profiles (
                profileId TEXT NOT NULL,
                vocabularyLevel TEXT,
                weakSpots TEXT NOT NULL,
                preferredQuestionTypes TEXT NOT NULL,
                commonMistakePatterns TEXT NOT NULL,
                checkpointSummariesJson TEXT NOT NULL DEFAULT '[]',
                analyticsSnapshotJson TEXT NOT NULL DEFAULT '{}',
                longTermInsightsJson TEXT NOT NULL DEFAULT '[]',
                goalProgressJson TEXT NOT NULL DEFAULT '{}',
                upgradeHealthJson TEXT NOT NULL DEFAULT '{}',
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(profileId)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS plan_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                generatedAt INTEGER NOT NULL,
                summary TEXT NOT NULL,
                parentPlanVersionId INTEGER,
                triggerType TEXT NOT NULL DEFAULT 'manual_refresh',
                sourceType TEXT NOT NULL DEFAULT 'LOCAL_FALLBACK',
                recommendedFocus TEXT NOT NULL,
                suggestedModes TEXT NOT NULL DEFAULT '',
                suggestedPace TEXT,
                reasonSummary TEXT,
                changeSummary TEXT,
                abnormalSignals TEXT NOT NULL DEFAULT '',
                severity TEXT NOT NULL DEFAULT 'MINOR',
                applyStatus TEXT NOT NULL DEFAULT 'APPLIED',
                isHighlightedAiChange INTEGER NOT NULL DEFAULT 0,
                executionEffect TEXT,
                confirmedAt INTEGER,
                rejectedAt INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_history_generatedAt ON plan_history(generatedAt)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS word_audio_assets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                wordId INTEGER NOT NULL,
                accent TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                remoteUrl TEXT,
                localPath TEXT,
                mimeType TEXT,
                checksum TEXT,
                status TEXT NOT NULL,
                fetchedAt INTEGER,
                lastPlayedAt INTEGER,
                lastError TEXT,
                failureCount INTEGER NOT NULL DEFAULT 0,
                sourceId TEXT,
                presetId TEXT,
                actualSourceType TEXT,
                namespace TEXT,
                assetState TEXT NOT NULL DEFAULT 'ready',
                taskId TEXT,
                FOREIGN KEY(wordId) REFERENCES words(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_word_audio_assets_wordId ON word_audio_assets(wordId)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_word_audio_assets_wordId_accent_sourceType ON word_audio_assets(wordId, accent, sourceType)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS voice_packs (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                locale TEXT NOT NULL,
                accent TEXT NOT NULL,
                engineType TEXT NOT NULL,
                version TEXT NOT NULL,
                downloadUrl TEXT,
                manifestUrl TEXT,
                checksumsUrl TEXT,
                installDir TEXT,
                archiveChecksum TEXT,
                installedSizeBytes INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS import_batches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bookId TEXT NOT NULL,
                fileName TEXT NOT NULL,
                sheetName TEXT,
                diagnosisSnapshotJson TEXT,
                parserMode TEXT NOT NULL,
                totalRows INTEGER NOT NULL,
                importedRows INTEGER NOT NULL,
                skippedRows INTEGER NOT NULL,
                aiNormalizedCount INTEGER NOT NULL,
                aiCompletedCount INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun assertHasColumn(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
    ) {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == columnName) {
                    found = true
                    break
                }
            }
            assertTrue("Missing column $columnName on $tableName", found)
        }
    }

    private fun assertMissingColumn(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
    ) {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == columnName) {
                    found = true
                    break
                }
            }
            assertFalse("Column $columnName should not exist on $tableName", found)
        }
    }
}
