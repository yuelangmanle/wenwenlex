package com.yueliangmanle.danci.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
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
}
