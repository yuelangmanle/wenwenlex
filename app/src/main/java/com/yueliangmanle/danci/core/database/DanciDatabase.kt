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
import com.yueliangmanle.danci.core.database.dao.AudioGenerationJobDao
import com.yueliangmanle.danci.core.database.dao.BookDao
import com.yueliangmanle.danci.core.database.dao.ImportBatchDao
import com.yueliangmanle.danci.core.database.dao.PhoneticEnrichmentJobDao
import com.yueliangmanle.danci.core.database.dao.StudyDao
import com.yueliangmanle.danci.core.database.dao.VoicePackDao
import com.yueliangmanle.danci.core.database.dao.WordDao
import com.yueliangmanle.danci.core.database.dao.WordAudioAssetDao
import com.yueliangmanle.danci.core.database.entity.AiProviderProfileEntity
import com.yueliangmanle.danci.core.database.entity.AudioGenerationJobEntity
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
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity
import com.yueliangmanle.danci.core.database.entity.WeeklySummaryEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.database.entity.WordAudioAssetEntity
import java.time.Instant

private const val CURRENT_DANCI_DB_VERSION = 9

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
        AudioGenerationJobEntity::class,
        ImportBatchEntity::class,
        PhoneticEnrichmentJobEntity::class,
        WordAudioAssetEntity::class,
        VoicePackEntity::class,
    ],
    version = CURRENT_DANCI_DB_VERSION,
    exportSchema = false,
)
@TypeConverters(DanciTypeConverters::class)
abstract class DanciDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun wordAudioAssetDao(): WordAudioAssetDao
    abstract fun audioGenerationJobDao(): AudioGenerationJobDao
    abstract fun bookDao(): BookDao
    abstract fun studyDao(): StudyDao
    abstract fun aiProviderProfileDao(): AiProviderProfileDao
    abstract fun importBatchDao(): ImportBatchDao
    abstract fun phoneticEnrichmentJobDao(): PhoneticEnrichmentJobDao
    abstract fun voicePackDao(): VoicePackDao
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
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .addMigrations(MIGRATION_5_9)
            .addMigrations(MIGRATION_6_9)
            .addMigrations(MIGRATION_7_9)
            .addMigrations(MIGRATION_8_9)
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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
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
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE learning_records ADD COLUMN reviewStage INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE learning_records ADD COLUMN learningStage TEXT NOT NULL DEFAULT 'UNSEEN'")
        db.execSQL("ALTER TABLE learning_records ADD COLUMN introducedAt INTEGER")
        db.execSQL("ALTER TABLE learning_records ADD COLUMN lastMistakeAt INTEGER")
        db.execSQL("ALTER TABLE learning_records ADD COLUMN lastFuzzyAt INTEGER")
        db.execSQL("ALTER TABLE learning_records ADD COLUMN lastStudyMode TEXT")
        db.execSQL("ALTER TABLE learning_records ADD COLUMN currentGroupPassState TEXT")

        db.execSQL("ALTER TABLE study_sessions ADD COLUMN scopeType TEXT NOT NULL DEFAULT 'book'")
        db.execSQL("ALTER TABLE study_sessions ADD COLUMN scopeRef TEXT")
        db.execSQL("ALTER TABLE study_sessions ADD COLUMN groupSize INTEGER NOT NULL DEFAULT 5")
        db.execSQL("ALTER TABLE study_sessions ADD COLUMN currentGroupIndex INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createAudioGenerationJobsTable(db)
    }
}

val MIGRATION_5_9 = object : Migration(5, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateLegacySchemaToCurrent(db)
    }
}

val MIGRATION_6_9 = object : Migration(6, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateLegacySchemaToCurrent(db)
    }
}

val MIGRATION_7_9 = object : Migration(7, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateLegacySchemaToCurrent(db)
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateLegacySchemaToCurrent(db)
    }
}

private fun migrateLegacySchemaToCurrent(db: SupportSQLiteDatabase) {
    db.execSQL("PRAGMA foreign_keys=OFF")
    try {
        rebuildStudySessionsTable(db)
        rebuildLearningRecordsTable(db)
        rebuildLearnerProfilesTable(db)
        rebuildPlanHistoryTable(db)
        rebuildWordAudioAssetsTable(db)
        rebuildVoicePacksTable(db)
        rebuildImportBatchesTable(db)
        createAudioGenerationJobsTable(db)
    } finally {
        db.execSQL("PRAGMA foreign_keys=ON")
    }
}

private fun rebuildStudySessionsTable(db: SupportSQLiteDatabase) {
    val columns = tableColumns(db, "study_sessions")
    rebuildTable(
        db = db,
        tableName = "study_sessions",
        createTableSql = CREATE_STUDY_SESSIONS_TABLE_SQL,
        insertColumns = listOf(
            "id",
            "mode",
            "targetBookId",
            "scopeType",
            "scopeRef",
            "groupSize",
            "currentGroupIndex",
            "startedAt",
            "finishedAt",
            "plannedCount",
            "completedCount",
            "correctCount",
            "wrongCount",
            "strategySnapshot",
        ),
        selectExpressions = listOf(
            existingColumnOrDefault(columns, "id", "NULL"),
            existingColumnOrDefault(columns, "mode", "'review'"),
            existingColumnOrDefault(columns, "targetBookId", "NULL"),
            existingColumnOrDefault(columns, "scopeType", "'book'"),
            existingColumnOrDefault(columns, "scopeRef", "NULL"),
            existingColumnOrDefault(columns, "groupSize", "5"),
            existingColumnOrDefault(columns, "currentGroupIndex", "0"),
            existingColumnOrDefault(columns, "startedAt", "0"),
            existingColumnOrDefault(columns, "finishedAt", "NULL"),
            existingColumnOrDefault(columns, "plannedCount", "0"),
            existingColumnOrDefault(columns, "completedCount", "0"),
            existingColumnOrDefault(columns, "correctCount", "0"),
            existingColumnOrDefault(columns, "wrongCount", "0"),
            existingColumnOrDefault(columns, "strategySnapshot", "NULL"),
        ),
    )
}

private fun rebuildLearningRecordsTable(db: SupportSQLiteDatabase) {
    val columns = tableColumns(db, "learning_records")
    rebuildTable(
        db = db,
        tableName = "learning_records",
        createTableSql = CREATE_LEARNING_RECORDS_TABLE_SQL,
        insertColumns = listOf(
            "wordId",
            "mastery",
            "familiarityState",
            "reviewStage",
            "learningStage",
            "introducedAt",
            "nextReviewAt",
            "reviewCount",
            "lapseCount",
            "consecutiveCorrectCount",
            "lastReviewedAt",
            "lastOutcome",
            "lastMistakeAt",
            "lastFuzzyAt",
            "lastStudyMode",
            "currentGroupPassState",
            "confusionWeight",
            "similarSpellingWeight",
        ),
        selectExpressions = listOf(
            existingColumnOrDefault(columns, "wordId", "NULL"),
            existingColumnOrDefault(columns, "mastery", "0"),
            existingColumnOrDefault(columns, "familiarityState", "'NEW'"),
            existingColumnOrDefault(columns, "reviewStage", "0"),
            existingColumnOrDefault(columns, "learningStage", "'UNSEEN'"),
            existingColumnOrDefault(columns, "introducedAt", "NULL"),
            existingColumnOrDefault(columns, "nextReviewAt", "NULL"),
            existingColumnOrDefault(columns, "reviewCount", "0"),
            existingColumnOrDefault(columns, "lapseCount", "0"),
            existingColumnOrDefault(columns, "consecutiveCorrectCount", "0"),
            existingColumnOrDefault(columns, "lastReviewedAt", "NULL"),
            existingColumnOrDefault(columns, "lastOutcome", "NULL"),
            existingColumnOrDefault(columns, "lastMistakeAt", "NULL"),
            existingColumnOrDefault(columns, "lastFuzzyAt", "NULL"),
            existingColumnOrDefault(columns, "lastStudyMode", "NULL"),
            existingColumnOrDefault(columns, "currentGroupPassState", "NULL"),
            existingColumnOrDefault(columns, "confusionWeight", "0"),
            existingColumnOrDefault(columns, "similarSpellingWeight", "0"),
        ),
        indexSqls = listOf(CREATE_LEARNING_RECORDS_NEXT_REVIEW_INDEX_SQL),
    )
}

private fun rebuildLearnerProfilesTable(db: SupportSQLiteDatabase) {
    val columns = tableColumns(db, "learner_profiles")
    rebuildTable(
        db = db,
        tableName = "learner_profiles",
        createTableSql = CREATE_LEARNER_PROFILES_TABLE_SQL,
        insertColumns = listOf(
            "profileId",
            "vocabularyLevel",
            "weakSpots",
            "preferredQuestionTypes",
            "commonMistakePatterns",
            "updatedAt",
        ),
        selectExpressions = listOf(
            existingColumnOrDefault(columns, "profileId", "'default'"),
            existingColumnOrDefault(columns, "vocabularyLevel", "NULL"),
            existingColumnOrDefault(columns, "weakSpots", "''"),
            existingColumnOrDefault(columns, "preferredQuestionTypes", "''"),
            existingColumnOrDefault(columns, "commonMistakePatterns", "''"),
            existingColumnOrDefault(columns, "updatedAt", "0"),
        ),
    )
}

private fun rebuildPlanHistoryTable(db: SupportSQLiteDatabase) {
    val columns = tableColumns(db, "plan_history")
    rebuildTable(
        db = db,
        tableName = "plan_history",
        createTableSql = CREATE_PLAN_HISTORY_TABLE_SQL,
        insertColumns = listOf(
            "id",
            "generatedAt",
            "summary",
            "recommendedFocus",
            "suggestedPace",
            "executionEffect",
        ),
        selectExpressions = listOf(
            existingColumnOrDefault(columns, "id", "NULL"),
            existingColumnOrDefault(columns, "generatedAt", "0"),
            existingColumnOrDefault(columns, "summary", "''"),
            existingColumnOrDefault(columns, "recommendedFocus", "''"),
            existingColumnOrDefault(columns, "suggestedPace", "NULL"),
            existingColumnOrDefault(columns, "executionEffect", "NULL"),
        ),
        indexSqls = listOf(CREATE_PLAN_HISTORY_GENERATED_AT_INDEX_SQL),
    )
}

private fun rebuildWordAudioAssetsTable(db: SupportSQLiteDatabase) {
    val columns = tableColumns(db, "word_audio_assets")
    rebuildTable(
        db = db,
        tableName = "word_audio_assets",
        createTableSql = CREATE_WORD_AUDIO_ASSETS_TABLE_SQL,
        insertColumns = listOf(
            "id",
            "wordId",
            "accent",
            "sourceType",
            "remoteUrl",
            "localPath",
            "mimeType",
            "checksum",
            "status",
            "fetchedAt",
            "lastPlayedAt",
            "lastError",
            "failureCount",
        ),
        selectExpressions = listOf(
            existingColumnOrDefault(columns, "id", "NULL"),
            existingColumnOrDefault(columns, "wordId", "NULL"),
            existingColumnOrDefault(columns, "accent", "'us'"),
            existingColumnOrDefault(columns, "sourceType", "'dictionary'"),
            existingColumnOrDefault(columns, "remoteUrl", "NULL"),
            existingColumnOrDefault(columns, "localPath", "NULL"),
            existingColumnOrDefault(columns, "mimeType", "NULL"),
            existingColumnOrDefault(columns, "checksum", "NULL"),
            existingColumnOrDefault(columns, "status", "'ready'"),
            existingColumnOrDefault(columns, "fetchedAt", "NULL"),
            existingColumnOrDefault(columns, "lastPlayedAt", "NULL"),
            existingColumnOrDefault(columns, "lastError", "NULL"),
            existingColumnOrDefault(columns, "failureCount", "0"),
        ),
        indexSqls = listOf(
            CREATE_WORD_AUDIO_ASSETS_WORD_ID_INDEX_SQL,
            CREATE_WORD_AUDIO_ASSETS_WORD_ACCENT_SOURCE_INDEX_SQL,
        ),
    )
}

private fun rebuildVoicePacksTable(db: SupportSQLiteDatabase) {
    val columns = tableColumns(db, "voice_packs")
    rebuildTable(
        db = db,
        tableName = "voice_packs",
        createTableSql = CREATE_VOICE_PACKS_TABLE_SQL,
        insertColumns = listOf(
            "id",
            "name",
            "locale",
            "accent",
            "engineType",
            "version",
            "downloadUrl",
            "manifestUrl",
            "installDir",
            "archiveChecksum",
            "installedSizeBytes",
            "status",
            "isActive",
            "createdAt",
            "updatedAt",
        ),
        selectExpressions = listOf(
            existingColumnOrDefault(columns, "id", "''"),
            existingColumnOrDefault(columns, "name", "''"),
            existingColumnOrDefault(columns, "locale", "''"),
            existingColumnOrDefault(columns, "accent", "''"),
            existingColumnOrDefault(columns, "engineType", "''"),
            existingColumnOrDefault(columns, "version", "'1'"),
            existingColumnOrDefault(columns, "downloadUrl", "NULL"),
            existingColumnOrDefault(columns, "manifestUrl", "NULL"),
            existingColumnOrDefault(columns, "installDir", "NULL"),
            existingColumnOrDefault(columns, "archiveChecksum", "NULL"),
            existingColumnOrDefault(columns, "installedSizeBytes", "0"),
            existingColumnOrDefault(columns, "status", "'idle'"),
            existingColumnOrDefault(columns, "isActive", "0"),
            existingColumnOrDefault(columns, "createdAt", "0"),
            existingColumnOrDefault(columns, "updatedAt", "0"),
        ),
    )
}

private fun rebuildImportBatchesTable(db: SupportSQLiteDatabase) {
    val columns = tableColumns(db, "import_batches")
    rebuildTable(
        db = db,
        tableName = "import_batches",
        createTableSql = CREATE_IMPORT_BATCHES_TABLE_SQL,
        insertColumns = listOf(
            "id",
            "bookId",
            "fileName",
            "sheetName",
            "parserMode",
            "totalRows",
            "importedRows",
            "skippedRows",
            "aiNormalizedCount",
            "aiCompletedCount",
            "createdAt",
        ),
        selectExpressions = listOf(
            existingColumnOrDefault(columns, "id", "NULL"),
            existingColumnOrDefault(columns, "bookId", "''"),
            existingColumnOrDefault(columns, "fileName", "''"),
            existingColumnOrDefault(columns, "sheetName", "NULL"),
            existingColumnOrDefault(columns, "parserMode", "'strict'"),
            existingColumnOrDefault(columns, "totalRows", "0"),
            existingColumnOrDefault(columns, "importedRows", "0"),
            existingColumnOrDefault(columns, "skippedRows", "0"),
            existingColumnOrDefault(columns, "aiNormalizedCount", "0"),
            existingColumnOrDefault(columns, "aiCompletedCount", "0"),
            existingColumnOrDefault(columns, "createdAt", "0"),
        ),
    )
}

private fun createAudioGenerationJobsTable(db: SupportSQLiteDatabase) {
    db.execSQL(CREATE_AUDIO_GENERATION_JOBS_TABLE_SQL)
}

private fun rebuildTable(
    db: SupportSQLiteDatabase,
    tableName: String,
    createTableSql: String,
    insertColumns: List<String>,
    selectExpressions: List<String>,
    indexSqls: List<String> = emptyList(),
) {
    if (!tableExists(db, tableName)) {
        db.execSQL(createTableSql)
        indexSqls.forEach(db::execSQL)
        return
    }
    val legacyTableName = "${tableName}_legacy_migration"
    db.execSQL("DROP TABLE IF EXISTS $legacyTableName")
    db.execSQL("ALTER TABLE $tableName RENAME TO $legacyTableName")
    indexSqls.mapNotNull(::indexNameFromCreateSql).forEach { indexName ->
        db.execSQL("DROP INDEX IF EXISTS $indexName")
    }
    db.execSQL(createTableSql)
    db.execSQL(
        """
        INSERT INTO $tableName (${insertColumns.joinToString(", ")})
        SELECT ${selectExpressions.joinToString(", ")}
        FROM $legacyTableName
        """.trimIndent(),
    )
    indexSqls.forEach(db::execSQL)
    db.execSQL("DROP TABLE IF EXISTS $legacyTableName")
}

private fun existingColumnOrDefault(
    columns: Set<String>,
    columnName: String,
    defaultSql: String,
): String = if (columns.contains(columnName)) columnName else defaultSql

private fun indexNameFromCreateSql(createIndexSql: String): String? =
    Regex("""CREATE INDEX IF NOT EXISTS\s+(\S+)\s+ON""", RegexOption.IGNORE_CASE)
        .find(createIndexSql)
        ?.groupValues
        ?.getOrNull(1)

private fun tableExists(
    db: SupportSQLiteDatabase,
    tableName: String,
): Boolean =
    db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$tableName'").use { cursor ->
        cursor.moveToFirst()
    }

private fun tableColumns(
    db: SupportSQLiteDatabase,
    tableName: String,
): Set<String> {
    if (!tableExists(db, tableName)) {
        return emptySet()
    }
    val columns = linkedSetOf<String>()
    db.query("PRAGMA table_info($tableName)").use { cursor ->
        while (cursor.moveToNext()) {
            columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
        }
    }
    return columns
}

private val CREATE_STUDY_SESSIONS_TABLE_SQL =
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
    """.trimIndent()

private val CREATE_LEARNING_RECORDS_TABLE_SQL =
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
    """.trimIndent()

private val CREATE_LEARNER_PROFILES_TABLE_SQL =
    """
    CREATE TABLE IF NOT EXISTS learner_profiles (
        profileId TEXT NOT NULL,
        vocabularyLevel TEXT,
        weakSpots TEXT NOT NULL,
        preferredQuestionTypes TEXT NOT NULL,
        commonMistakePatterns TEXT NOT NULL,
        updatedAt INTEGER NOT NULL,
        PRIMARY KEY(profileId)
    )
    """.trimIndent()

private val CREATE_PLAN_HISTORY_TABLE_SQL =
    """
    CREATE TABLE IF NOT EXISTS plan_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        generatedAt INTEGER NOT NULL,
        summary TEXT NOT NULL,
        recommendedFocus TEXT NOT NULL,
        suggestedPace TEXT,
        executionEffect TEXT
    )
    """.trimIndent()

private val CREATE_WORD_AUDIO_ASSETS_TABLE_SQL =
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
        FOREIGN KEY(wordId) REFERENCES words(id) ON UPDATE NO ACTION ON DELETE CASCADE
    )
    """.trimIndent()

private val CREATE_VOICE_PACKS_TABLE_SQL =
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
        installDir TEXT,
        archiveChecksum TEXT,
        installedSizeBytes INTEGER NOT NULL DEFAULT 0,
        status TEXT NOT NULL,
        isActive INTEGER NOT NULL DEFAULT 0,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL
    )
    """.trimIndent()

private val CREATE_IMPORT_BATCHES_TABLE_SQL =
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
    """.trimIndent()

private val CREATE_AUDIO_GENERATION_JOBS_TABLE_SQL =
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
    """.trimIndent()

private const val CREATE_LEARNING_RECORDS_NEXT_REVIEW_INDEX_SQL =
    "CREATE INDEX IF NOT EXISTS index_learning_records_nextReviewAt ON learning_records(nextReviewAt)"

private const val CREATE_PLAN_HISTORY_GENERATED_AT_INDEX_SQL =
    "CREATE INDEX IF NOT EXISTS index_plan_history_generatedAt ON plan_history(generatedAt)"

private const val CREATE_WORD_AUDIO_ASSETS_WORD_ID_INDEX_SQL =
    "CREATE INDEX IF NOT EXISTS index_word_audio_assets_wordId ON word_audio_assets(wordId)"

private const val CREATE_WORD_AUDIO_ASSETS_WORD_ACCENT_SOURCE_INDEX_SQL =
    "CREATE INDEX IF NOT EXISTS index_word_audio_assets_wordId_accent_sourceType ON word_audio_assets(wordId, accent, sourceType)"

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
