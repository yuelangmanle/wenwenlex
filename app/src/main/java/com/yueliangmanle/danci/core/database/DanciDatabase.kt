package com.yueliangmanle.danci.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yueliangmanle.danci.core.database.dao.AiProviderProfileDao
import com.yueliangmanle.danci.core.database.dao.BookDao
import com.yueliangmanle.danci.core.database.dao.ImportBatchDao
import com.yueliangmanle.danci.core.database.dao.PhoneticEnrichmentJobDao
import com.yueliangmanle.danci.core.database.dao.StudyDao
import com.yueliangmanle.danci.core.database.dao.WordDao
import com.yueliangmanle.danci.core.database.entity.AiProviderProfileEntity
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.database.entity.ConfusionEdgeEntity
import com.yueliangmanle.danci.core.database.entity.DailySummaryEntity
import com.yueliangmanle.danci.core.database.entity.ImportBatchEntity
import com.yueliangmanle.danci.core.database.entity.LearnerProfileEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.PlanHistoryEntity
import com.yueliangmanle.danci.core.database.entity.PhoneticEnrichmentJobEntity
import com.yueliangmanle.danci.core.database.entity.StudyEventEntity
import com.yueliangmanle.danci.core.database.entity.StudySessionEntity
import com.yueliangmanle.danci.core.database.entity.WeeklySummaryEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import java.time.Instant

@Database(
    entities = [
        WordEntity::class,
        BookEntity::class,
        BookWordEntity::class,
        LearningRecordEntity::class,
        StudySessionEntity::class,
        StudyEventEntity::class,
        DailySummaryEntity::class,
        WeeklySummaryEntity::class,
        LearnerProfileEntity::class,
        PlanHistoryEntity::class,
        ConfusionEdgeEntity::class,
        AiProviderProfileEntity::class,
        ImportBatchEntity::class,
        PhoneticEnrichmentJobEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(DanciTypeConverters::class)
abstract class DanciDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun bookDao(): BookDao
    abstract fun studyDao(): StudyDao
    abstract fun aiProviderProfileDao(): AiProviderProfileDao
    abstract fun importBatchDao(): ImportBatchDao
    abstract fun phoneticEnrichmentJobDao(): PhoneticEnrichmentJobDao
}

private const val DANCI_DB_NAME = "danci.db"

private object DanciDatabaseHolder {
    @Volatile
    var instance: DanciDatabase? = null
}

fun buildDanciDatabase(context: Context): DanciDatabase {
    DanciDatabaseHolder.instance?.let { return it }
    return synchronized(DanciDatabaseHolder) {
        DanciDatabaseHolder.instance ?: Room.databaseBuilder(
            context.applicationContext,
            DanciDatabase::class.java,
            DANCI_DB_NAME,
        )
            .addMigrations(MIGRATION_1_2)
            .build().also { database ->
            DanciDatabaseHolder.instance = database
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE words ADD COLUMN phoneticUk TEXT")
        db.execSQL("ALTER TABLE words ADD COLUMN phoneticUs TEXT")
        db.execSQL("ALTER TABLE words ADD COLUMN phoneticSource TEXT NOT NULL DEFAULT 'empty'")
        db.execSQL("ALTER TABLE words ADD COLUMN phoneticStatus TEXT NOT NULL DEFAULT 'empty'")
        db.execSQL("ALTER TABLE words ADD COLUMN phoneticUpdatedAt INTEGER")
        db.execSQL(
            """
            UPDATE words
            SET phoneticUk = phonetic,
                phoneticSource = CASE
                    WHEN phonetic IS NULL OR TRIM(phonetic) = '' THEN 'empty'
                    ELSE 'legacy'
                END,
                phoneticStatus = CASE
                    WHEN phonetic IS NULL OR TRIM(phonetic) = '' THEN 'empty'
                    ELSE 'partial'
                END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ai_provider_profiles (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                providerType TEXT NOT NULL,
                baseUrl TEXT NOT NULL,
                model TEXT NOT NULL,
                enabled INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastValidatedAt INTEGER,
                PRIMARY KEY(id)
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS phonetic_enrichment_jobs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                scopeType TEXT NOT NULL,
                scopeRef TEXT NOT NULL,
                profileId TEXT,
                fillMode TEXT NOT NULL,
                status TEXT NOT NULL,
                totalCount INTEGER NOT NULL,
                completedCount INTEGER NOT NULL,
                failedCount INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

class DanciTypeConverters {
    private val separator = '\u001F'

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value.orEmpty().joinToString(separator.toString())

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value
            ?.takeIf { it.isNotBlank() }
            ?.split(separator)
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
