package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.room.withTransaction
import com.yueliangmanle.danci.core.backup.BackupArchive
import com.yueliangmanle.danci.core.backup.BackupExporter
import com.yueliangmanle.danci.core.backup.BackupImporter
import com.yueliangmanle.danci.core.backup.BackupSnapshot
import com.yueliangmanle.danci.core.database.DanciDatabase
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import java.io.File
import java.time.Instant

data class BackupFileResult(
    val file: File,
    val archive: BackupArchive,
)

data class RestoreBackupResult(
    val restoredAt: Instant,
    val file: File,
)

class BackupRepository(
    private val context: Context,
    private val database: DanciDatabase,
    private val settingsRepository: SettingsRepository,
    private val aiMemoryRepository: AiMemoryRepository,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    suspend fun exportToLocalFile(): BackupFileResult {
        val archive = BackupExporter(
            snapshotProvider = {
                BackupSnapshot(
                    settings = settingsRepository.getSettings(),
                    books = database.bookDao().getAllBooks(),
                    bookWords = database.bookDao().getAllBookWordCrossRefs(),
                    words = database.wordDao().getAllWords(),
                    learningRecords = database.studyDao().getAllLearningRecords(),
                    studySessions = database.studyDao().getAllStudySessions(),
                    studyEvents = database.studyDao().getAllStudyEvents(),
                    aiMemorySummary = aiMemoryRepository.refreshMemorySummary(),
                )
            },
            nowProvider = nowProvider,
        ).export()

        val file = backupDirectory().resolve("wenwenlex-backup-${nowProvider().toEpochMilli()}.zip")
        file.parentFile?.mkdirs()
        file.writeBytes(archive.zippedBytes)
        return BackupFileResult(
            file = file,
            archive = archive,
        )
    }

    suspend fun restoreLatestBackup(): RestoreBackupResult {
        val file = requireNotNull(latestBackupFile()) { "还没有可恢复的本地备份" }
        val imported = BackupImporter().import(file.readBytes())
        database.withTransaction {
            database.studyDao().clearStudyEvents()
            database.studyDao().clearStudySessions()
            database.studyDao().clearLearningRecords()
            database.studyDao().clearDailySummaries()
            database.studyDao().clearWeeklySummaries()
            database.studyDao().clearLearnerProfiles()
            database.studyDao().clearPlanHistory()
            database.studyDao().clearConfusionEdges()
            database.bookDao().clearBookWordCrossRefs()
            database.bookDao().clearBooks()
            database.wordDao().clearWords()

            if (imported.snapshot.words.isNotEmpty()) {
                database.wordDao().insertWords(imported.snapshot.words)
            }
            if (imported.snapshot.books.isNotEmpty()) {
                database.bookDao().insertBooks(imported.snapshot.books)
            }
            if (imported.snapshot.bookWords.isNotEmpty()) {
                database.bookDao().insertBookWordCrossRefs(imported.snapshot.bookWords)
            }
            imported.snapshot.learningRecords.forEach { record ->
                database.studyDao().upsertLearningRecord(record)
            }
            if (imported.snapshot.studySessions.isNotEmpty()) {
                database.studyDao().insertStudySessions(imported.snapshot.studySessions)
            }
            if (imported.snapshot.studyEvents.isNotEmpty()) {
                database.studyDao().insertStudyEvents(imported.snapshot.studyEvents)
            }
        }

        applySettings(imported.snapshot.settings)
        RoomStudyRepository(database.studyDao()).saveAiMemorySummary(imported.snapshot.aiMemorySummary)

        return RestoreBackupResult(
            restoredAt = nowProvider(),
            file = file,
        )
    }

    fun latestBackupFile(): File? =
        backupDirectory()
            .takeIf(File::exists)
            ?.listFiles { file -> file.extension == "zip" }
            ?.maxByOrNull(File::lastModified)

    fun backupDirectory(): File =
        context.getExternalFilesDir("backups")
            ?: File(context.filesDir, "backups")

    private suspend fun applySettings(settings: AppSettings) {
        settingsRepository.updateDailyGoal(settings.dailyGoal)
        settingsRepository.updateActiveBookId(settings.activeBookId)
        settingsRepository.updateAiEnabled(settings.aiEnabled)
        settingsRepository.updateAiBaseUrl(settings.aiBaseUrl)
        settingsRepository.updateAiModel(settings.aiModel)
        settingsRepository.updateAiPlanAdjustmentEnabled(settings.aiPlanAdjustmentEnabled)
        settingsRepository.updateAiSessionCheckpointEnabled(settings.aiSessionCheckpointEnabled)
        settingsRepository.updateReminderEnabled(settings.reminderEnabled)
        settingsRepository.updateReminderTime(settings.reminderHour, settings.reminderMinute)
    }
}

fun buildBackupRepository(context: Context): BackupRepository =
    BackupRepository(
        context = context.applicationContext,
        database = buildDanciDatabase(context),
        settingsRepository = buildSettingsRepository(context),
        aiMemoryRepository = buildAiMemoryRepository(context),
    )
