package com.yueliangmanle.danci.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DanciDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DanciDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration4To5_backfillsExistingPlanHistoryRows() {
        val databaseName = "danci-migration-test"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(databaseName)
        helper.createDatabase(databaseName, 4).apply {
            execSQL(
                """
                INSERT INTO learner_profiles (
                    profileId,
                    vocabularyLevel,
                    weakSpots,
                    preferredQuestionTypes,
                    commonMistakePatterns,
                    updatedAt
                ) VALUES (
                    'default',
                    '提升中',
                    'abandonprecise',
                    'quiz',
                    '',
                    1774008600000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO plan_history (
                    id,
                    generatedAt,
                    summary,
                    recommendedFocus,
                    suggestedPace,
                    executionEffect
                ) VALUES (
                    1,
                    1774008600000,
                    '先稳住复习节奏',
                    'abandonprecise',
                    'steady',
                    '正确率回升'
                )
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            databaseName,
            5,
            true,
            MIGRATION_4_5,
        )

        val cursor = migratedDb.query(
            """
            SELECT parentPlanVersionId, severity, applyStatus, triggerType, sourceType
            FROM plan_history
            WHERE id = 1
            """.trimIndent(),
        )
        cursor.use {
            check(cursor.moveToFirst())
            val parentPlanVersionId = if (cursor.isNull(0)) null else cursor.getLong(0)
            val severity = cursor.getString(1)
            val applyStatus = cursor.getString(2)
            val triggerType = cursor.getString(3)
            val sourceType = cursor.getString(4)
            assertNull(parentPlanVersionId)
            assertEquals("MINOR", severity)
            assertEquals("APPLIED", applyStatus)
            assertEquals("manual_refresh", triggerType)
            assertEquals("LOCAL_FALLBACK", sourceType)
        }

        migratedDb.query(
            """
            SELECT checkpointSummariesJson
            FROM learner_profiles
            WHERE profileId = 'default'
            """.trimIndent(),
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("[]", cursor.getString(0))
        }
    }

    @Test
    fun migration5To6_backfillsAnalyticsSnapshotAndLongTermInsights() {
        val databaseName = "danci-migration-test-v6"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(databaseName)
        helper.createDatabase(databaseName, 5).apply {
            execSQL(
                """
                INSERT INTO learner_profiles (
                    profileId,
                    vocabularyLevel,
                    weakSpots,
                    preferredQuestionTypes,
                    commonMistakePatterns,
                    checkpointSummariesJson,
                    updatedAt
                ) VALUES (
                    'default',
                    '提升中',
                    'abandonprecise',
                    'quiz',
                    '',
                    '[]',
                    1774008600000
                )
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            databaseName,
            6,
            true,
            MIGRATION_5_6,
        )

        migratedDb.query(
            """
            SELECT analyticsSnapshotJson, longTermInsightsJson, checkpointSummariesJson
            FROM learner_profiles
            WHERE profileId = 'default'
            """.trimIndent(),
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("{}", cursor.getString(0))
            assertEquals("[]", cursor.getString(1))
            assertEquals("[]", cursor.getString(2))
        }
    }

    @Test
    fun migration6To7_addsLearningSignalColumnsAndGoalProgressJson() {
        val databaseName = "danci-migration-test-v7"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(databaseName)
        helper.createDatabase(databaseName, 6).apply {
            execSQL(
                """
                INSERT INTO words (
                    id,
                    lemma,
                    phoneticSource,
                    phoneticStatus,
                    partOfSpeech,
                    meanings,
                    synonyms,
                    antonyms,
                    similarWords,
                    confusingWords,
                    wordForms,
                    tags
                ) VALUES (
                    1,
                    'abandon',
                    'legacy',
                    'partial',
                    '',
                    '放弃',
                    '',
                    '',
                    '',
                    '',
                    '',
                    ''
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO learning_records (
                    wordId,
                    mastery,
                    familiarityState,
                    nextReviewAt,
                    reviewCount,
                    lapseCount,
                    consecutiveCorrectCount,
                    lastReviewedAt,
                    lastOutcome,
                    confusionWeight,
                    similarSpellingWeight
                ) VALUES (
                    1,
                    0.45,
                    '学习中',
                    NULL,
                    4,
                    1,
                    2,
                    1774008600000,
                    'correct',
                    0.1,
                    0.2
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO learner_profiles (
                    profileId,
                    vocabularyLevel,
                    weakSpots,
                    preferredQuestionTypes,
                    commonMistakePatterns,
                    checkpointSummariesJson,
                    analyticsSnapshotJson,
                    longTermInsightsJson,
                    updatedAt
                ) VALUES (
                    'default',
                    '提升中',
                    'abandon',
                    'quiz',
                    '',
                    '[]',
                    '{}',
                    '[]',
                    1774008600000
                )
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            databaseName,
            7,
            true,
            MIGRATION_6_7,
        )

        migratedDb.query(
            """
            SELECT
                forgettingRiskScore,
                reviewPriorityScore,
                proficiencyBand,
                lastResponseLatencyMs,
                averageResponseLatencyMs,
                consecutiveMistakeCount,
                lastMistakeAt
            FROM learning_records
            WHERE wordId = 1
            """.trimIndent(),
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(0f, cursor.getFloat(0))
            assertEquals(0f, cursor.getFloat(1))
            assertEquals("new", cursor.getString(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
            assertEquals(0, cursor.getInt(5))
            assertTrue(cursor.isNull(6))
        }

        migratedDb.query(
            """
            SELECT goalProgressJson, upgradeHealthJson
            FROM learner_profiles
            WHERE profileId = 'default'
            """.trimIndent(),
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("{}", cursor.getString(0))
            assertEquals("{}", cursor.getString(1))
        }
    }

    @Test
    fun migration7To8_createsPronunciationSourcesAndAudioGenerationTables() {
        val databaseName = "danci-migration-test-v8"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(databaseName)
        helper.createDatabase(databaseName, 7).apply {
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            databaseName,
            8,
            true,
            MIGRATION_7_8,
        )

        assertTrue(tableExists(migratedDb, "pronunciation_sources"))
        assertTrue(tableExists(migratedDb, "pronunciation_source_presets"))
        assertTrue(tableExists(migratedDb, "audio_generation_tasks"))
        assertTrue(tableExists(migratedDb, "audio_generation_task_items"))

        val columns = mutableSetOf<String>()
        migratedDb.query("PRAGMA table_info(`word_audio_assets`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex)
            }
        }
        assertTrue("sourceId" in columns)
        assertTrue("presetId" in columns)
        assertTrue("assetState" in columns)
        assertTrue("actualSourceType" in columns)
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        tableName: String,
    ): Boolean =
        db.query(
            """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table' AND name = ?
            """.trimIndent(),
            arrayOf(tableName),
        ).use { cursor ->
            cursor.moveToFirst()
        }
}
