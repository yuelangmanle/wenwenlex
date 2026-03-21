package com.yueliangmanle.danci.core.database

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DanciDatabaseMigrationTest {

    @Test
    fun migration4To5_backfillsExistingPlanHistoryRows() {
        val databaseName = "danci-migration-test"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(databaseName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(4) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS plan_history (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    generatedAt INTEGER NOT NULL,
                                    summary TEXT NOT NULL,
                                    recommendedFocus TEXT NOT NULL,
                                    suggestedPace TEXT,
                                    executionEffect TEXT
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS learner_profiles (
                                    profileId TEXT NOT NULL PRIMARY KEY,
                                    vocabularyLevel TEXT,
                                    weakSpots TEXT NOT NULL,
                                    preferredQuestionTypes TEXT NOT NULL,
                                    commonMistakePatterns TEXT NOT NULL,
                                    updatedAt INTEGER NOT NULL
                                )
                                """.trimIndent(),
                            )
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_history_generatedAt ON plan_history(generatedAt)")
                        }

                        override fun onUpgrade(
                            db: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )

        helper.writableDatabase.apply {
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
        helper.close()

        val migratedHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) {
                        MIGRATION_4_5.migrate(db)
                    }
                })
                .build(),
        )

        migratedHelper.writableDatabase.query(
            """
            SELECT parentPlanVersionId, severity, applyStatus
            FROM plan_history
            WHERE id = 1
            """.trimIndent(),
        ).use { cursor ->
            check(cursor.moveToFirst())
            val parentPlanVersionId = if (cursor.isNull(0)) null else cursor.getLong(0)
            val severity = cursor.getString(1)
            val applyStatus = cursor.getString(2)
            assertNull(parentPlanVersionId)
            assertEquals("MINOR", severity)
            assertEquals("APPLIED", applyStatus)
        }
        migratedHelper.close()
        context.deleteDatabase(databaseName)
    }
}
