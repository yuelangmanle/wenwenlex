package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.backup.BackupExporter
import com.yueliangmanle.danci.core.backup.BackupSnapshot
import com.yueliangmanle.danci.core.database.DanciDatabase
import com.yueliangmanle.danci.core.database.entity.AudioGenerationTaskEntity
import com.yueliangmanle.danci.core.database.entity.AudioGenerationTaskItemEntity
import com.yueliangmanle.danci.core.database.entity.PronunciationSourceEntity
import com.yueliangmanle.danci.core.database.entity.PronunciationSourcePresetEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudySession
import com.yueliangmanle.danci.core.model.Word
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    private var database: DanciDatabase? = null
    private var backupDirectory: java.io.File? = null

    @After
    fun tearDown() {
        database?.close()
        database = null
        backupDirectory?.deleteRecursively()
        backupDirectory = null
    }

    @Test
    fun restoreLatestBackup_appliesWeeklyAndPhaseSettings() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(
            context,
            DanciDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        database = db

        val settingsRepository = RecordingSettingsRepository()
        val aiMemoryRepository = AiMemoryRepository(
            studyRepository = FakeStudyRepository(),
            wordRepository = FakeWordRepository(),
            builtInWordsProvider = { emptyList() },
            nowProvider = { FIXED_NOW },
        )
        val repository = BackupRepository(
            context = context,
            database = db,
            settingsRepository = settingsRepository,
            aiMemoryRepository = aiMemoryRepository,
            nowProvider = { FIXED_NOW },
        )

        val backup = BackupExporter(
            snapshotProvider = {
                BackupSnapshot(
                    settings = AppSettings(
                        dailyGoal = 28,
                        weeklyGoal = 88,
                        phaseName = "冲刺阶段",
                        phaseTargetWords = 1600,
                    ),
                    aiMemorySummary = AiMemorySummary(),
                )
            },
            nowProvider = { FIXED_NOW },
        ).export()

        val backupDir = repository.backupDirectory().also {
            it.mkdirs()
            it.listFiles()?.forEach { file -> if (file.extension == "zip") file.delete() }
        }
        backupDirectory = backupDir
        val zipFile = backupDir.resolve("restore-settings-test.zip")
        zipFile.writeBytes(backup.zippedBytes)
        assertTrue(zipFile.exists())

        repository.restoreLatestBackup()

        assertEquals(28, settingsRepository.current.dailyGoal)
        assertEquals(88, settingsRepository.current.weeklyGoal)
        assertEquals("冲刺阶段", settingsRepository.current.phaseName)
        assertEquals(1600, settingsRepository.current.phaseTargetWords)
    }

    @Test
    fun restoreLatestBackup_restoresPronunciationSourcesAndGenerationTasks() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(
            context,
            DanciDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        database = db

        val repository = BackupRepository(
            context = context,
            database = db,
            settingsRepository = RecordingSettingsRepository(),
            aiMemoryRepository = AiMemoryRepository(
                studyRepository = FakeStudyRepository(),
                wordRepository = FakeWordRepository(),
                builtInWordsProvider = { emptyList() },
                nowProvider = { FIXED_NOW },
            ),
            nowProvider = { FIXED_NOW },
        )

        val backup = BackupExporter(
            snapshotProvider = {
                BackupSnapshot(
                    settings = AppSettings(),
                    pronunciationSources = listOf(
                        PronunciationSourceEntity(
                            id = "source-mimo-en",
                            name = "MiMo English",
                            sourceType = "cloud_tts",
                            accent = "us",
                            enabled = true,
                            isDefaultForWord = true,
                            isDefaultForLongText = false,
                            providerProfileId = "profile-mimo",
                            createdAt = FIXED_NOW,
                            updatedAt = FIXED_NOW,
                        ),
                    ),
                    pronunciationSourcePresets = listOf(
                        PronunciationSourcePresetEntity(
                            sourceId = "source-mimo-en",
                            presetId = "default_en",
                            displayName = "Default EN",
                            voice = "default_en",
                            isDefaultPreset = true,
                        ),
                    ),
                    audioGenerationTasks = listOf(
                        AudioGenerationTaskEntity(
                            id = "task-1",
                            sourceId = "source-mimo-en",
                            presetId = "default_en",
                            scopeType = "book",
                            scopeRef = "cet4",
                            status = "queued",
                            totalItems = 10,
                            completedItems = 2,
                            failedItems = 1,
                            createdAt = FIXED_NOW,
                            updatedAt = FIXED_NOW,
                        ),
                    ),
                    audioGenerationTaskItems = listOf(
                        AudioGenerationTaskItemEntity(
                            taskId = "task-1",
                            itemKey = "word-1",
                            wordId = 1L,
                            text = "abandon",
                            status = "failed",
                            failureReason = "timeout",
                            attemptCount = 1,
                        ),
                    ),
                    aiMemorySummary = AiMemorySummary(),
                )
            },
            nowProvider = { FIXED_NOW },
        ).export()

        val backupDir = repository.backupDirectory().also {
            it.mkdirs()
            it.listFiles()?.forEach { file -> if (file.extension == "zip") file.delete() }
        }
        backupDirectory = backupDir
        backupDir.resolve("restore-pronunciation-test.zip").writeBytes(backup.zippedBytes)

        repository.restoreLatestBackup()

        val restoredSources = db.pronunciationSourceDao().getAllSources()
        val restoredPresets = db.pronunciationSourceDao().getPresetsBySource("source-mimo-en")
        val restoredTasks = db.audioGenerationTaskDao().getAllTasks()
        val restoredItems = db.audioGenerationTaskDao().getItemsByTask("task-1")

        assertEquals(1, restoredSources.size)
        assertEquals("source-mimo-en", restoredSources.single().id)
        assertEquals(1, restoredPresets.size)
        assertEquals("default_en", restoredPresets.single().presetId)
        assertEquals(1, restoredTasks.size)
        assertEquals("task-1", restoredTasks.single().id)
        assertEquals(1, restoredItems.size)
        assertEquals("timeout", restoredItems.single().failureReason)
    }

    private class RecordingSettingsRepository : SettingsRepository {
        private val state = kotlinx.coroutines.flow.MutableStateFlow(AppSettings())
        var current: AppSettings = AppSettings()
            private set

        override val settings: Flow<AppSettings> = state

        override suspend fun getSettings(): AppSettings = current

        override suspend fun updateDailyGoal(dailyGoal: Int) {
            current = current.copy(dailyGoal = dailyGoal.coerceAtLeast(1))
            state.value = current
        }

        override suspend fun updateWeeklyGoal(weeklyGoal: Int) {
            current = current.copy(weeklyGoal = weeklyGoal.coerceAtLeast(1))
            state.value = current
        }

        override suspend fun updatePhaseName(phaseName: String?) {
            current = current.copy(phaseName = phaseName)
            state.value = current
        }

        override suspend fun updatePhaseTargetWords(phaseTargetWords: Int) {
            current = current.copy(phaseTargetWords = phaseTargetWords.coerceAtLeast(0))
            state.value = current
        }

        override suspend fun updateActiveBookId(bookId: String?) {
            current = current.copy(activeBookId = bookId)
            state.value = current
        }

        override suspend fun updateAiEnabled(enabled: Boolean) {
            current = current.copy(aiEnabled = enabled)
            state.value = current
        }

        override suspend fun updateAiBaseUrl(baseUrl: String) {
            current = current.copy(aiBaseUrl = baseUrl)
            state.value = current
        }

        override suspend fun updateAiModel(model: String) {
            current = current.copy(aiModel = model)
            state.value = current
        }

        override suspend fun updateDefaultAiProfileId(profileId: String?) {
            current = current.copy(defaultAiProfileId = profileId)
            state.value = current
        }

        override suspend fun updateWordHelpProfileId(profileId: String?) {
            current = current.copy(wordHelpProfileId = profileId)
            state.value = current
        }

        override suspend fun updatePlanAdjustmentProfileId(profileId: String?) {
            current = current.copy(planAdjustmentProfileId = profileId)
            state.value = current
        }

        override suspend fun updatePhoneticFillProfileId(profileId: String?) {
            current = current.copy(phoneticFillProfileId = profileId)
            state.value = current
        }

        override suspend fun updateAiPlanAdjustmentEnabled(enabled: Boolean) {
            current = current.copy(aiPlanAdjustmentEnabled = enabled)
            state.value = current
        }

        override suspend fun updateAiSessionCheckpointEnabled(enabled: Boolean) {
            current = current.copy(aiSessionCheckpointEnabled = enabled)
            state.value = current
        }

        override suspend fun updatePreferredPronunciationAccent(accent: String) {
            current = current.copy(preferredPronunciationAccent = accent)
            state.value = current
        }

        override suspend fun updatePronunciationMode(mode: String) {
            current = current.copy(pronunciationMode = mode)
            state.value = current
        }

        override suspend fun updateAllowCellularVoicePackDownload(enabled: Boolean) {
            current = current.copy(allowCellularVoicePackDownload = enabled)
            state.value = current
        }

        override suspend fun updateAutoCacheWordAudio(enabled: Boolean) {
            current = current.copy(autoCacheWordAudio = enabled)
            state.value = current
        }

        override suspend fun updateAudioCacheLimitMb(limitMb: Int) {
            current = current.copy(audioCacheLimitMb = limitMb)
            state.value = current
        }

        override suspend fun updateActiveVoicePackId(voicePackId: String?) {
            current = current.copy(activeVoicePackId = voicePackId)
            state.value = current
        }

        override suspend fun updateFallbackToSystemTts(enabled: Boolean) {
            current = current.copy(fallbackToSystemTts = enabled)
            state.value = current
        }

        override suspend fun updatePreferOfflineForLongText(enabled: Boolean) {
            current = current.copy(preferOfflineForLongText = enabled)
            state.value = current
        }

        override suspend fun updateReminderEnabled(enabled: Boolean) {
            current = current.copy(reminderEnabled = enabled)
            state.value = current
        }

        override suspend fun updateReminderTime(hour: Int, minute: Int) {
            current = current.copy(reminderHour = hour, reminderMinute = minute)
            state.value = current
        }
    }

    private class FakeStudyRepository : StudyRepository {
        override fun observeLearningRecord(wordId: Long): Flow<LearningRecord?> = flowOf(null)

        override suspend fun getLearningRecordOrDefault(wordId: Long): LearningRecord =
            defaultLearningRecord(wordId)

        override suspend fun getLearningRecordsForWord(wordId: Long): List<LearningRecord> = emptyList()

        override suspend fun getAllLearningRecords(): List<LearningRecord> = emptyList()

        override suspend fun getAllStudySessions(): List<StudySession> = emptyList()

        override suspend fun upsertLearningRecord(record: LearningRecord) = Unit

        override suspend fun startSession(session: StudySession): Long = 0L

        override suspend fun appendEvent(event: StudyEvent): Long = 0L

        override suspend fun getStudyEventsSince(since: Instant): List<StudyEvent> = emptyList()

        override suspend fun getAllStudyEvents(): List<StudyEvent> = emptyList()

        override suspend fun getRecentStudyEvents(limit: Int): List<StudyEvent> = emptyList()

        override suspend fun loadAiMemorySummary(
            dailyLimit: Int,
            weeklyLimit: Int,
            planLimit: Int,
            confusionLimit: Int,
        ): AiMemorySummary = AiMemorySummary()

        override suspend fun saveAiMemorySummary(summary: AiMemorySummary) = Unit
    }

    private class FakeWordRepository : WordRepository {
        override fun observeWords(query: String): Flow<List<Word>> = flowOf(emptyList())

        override suspend fun getWord(wordId: Long): Word? = null

        override suspend fun getWords(wordIds: List<Long>): List<Word> = emptyList()

        override suspend fun getAllWords(): List<Word> = emptyList()

        override suspend fun insertWord(word: Word): Long = word.id

        override suspend fun updateWord(word: Word) = Unit

        override suspend fun importWords(words: List<com.yueliangmanle.danci.core.importer.ImportedWord>): List<Long> =
            emptyList()
    }

    private companion object {
        val FIXED_NOW: Instant = Instant.parse("2026-03-22T10:00:00Z")
    }
}
