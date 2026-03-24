package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.AudioGenerationRepository
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.importer.ImportedBook
import com.yueliangmanle.danci.core.importer.ImportedWord
import com.yueliangmanle.danci.core.model.AudioGenerationJob
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobStatus
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobType
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationScope
import com.yueliangmanle.danci.core.worker.AudioGenerationController
import java.io.InputStream
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTaskCenterViewModelTest {
    @Test
    fun taskCenter_canPauseResumeAndCancelJob() = runTest {
        val repository = FakeAudioGenerationRepository(
            mutableListOf(
                sampleJob(
                    id = 1L,
                    totalCount = 20,
                    status = AudioGenerationJobStatus.QUEUED.storageValue,
                ),
            ),
        )
        val controller = FakeAudioGenerationController(repository)
        val viewModel = AudioTaskCenterViewModel(
            settingsRepository = FakeAudioTaskSettingsRepository(),
            repository = repository,
            controller = controller,
            bookRepository = FakeAudioTaskBookRepository(),
            wordRepository = FakeAudioTaskWordRepository(),
        )

        viewModel.pauseJob(1L)
        assertEquals(AudioGenerationJobStatus.PAUSED.storageValue, repository.get(1L)?.status)

        viewModel.resumeJob(1L)
        assertEquals(AudioGenerationJobStatus.QUEUED.storageValue, repository.get(1L)?.status)

        viewModel.cancelJob(1L)
        assertEquals(AudioGenerationJobStatus.CANCELLED.storageValue, repository.get(1L)?.status)
    }
}

private fun sampleJob(
    id: Long,
    totalCount: Int,
    status: String,
): AudioGenerationJob =
    AudioGenerationJob(
        id = id,
        jobType = AudioGenerationJobType.CLOUD_TTS_PREFETCH.storageValue,
        sourceType = "online_prebuilt_cache",
        scopeType = AudioGenerationScope.ACTIVE_BOOK.scopeType,
        scopeRef = "cet4",
        status = status,
        totalCount = totalCount,
        createdAt = Instant.parse("2026-03-24T10:00:00Z"),
        updatedAt = Instant.parse("2026-03-24T10:00:00Z"),
    )

private class FakeAudioGenerationRepository(
    private val jobs: MutableList<AudioGenerationJob> = mutableListOf(),
) : AudioGenerationRepository {
    override suspend fun insert(job: AudioGenerationJob): Long {
        val resolvedId = job.id.takeIf { it > 0 } ?: (jobs.maxOfOrNull(AudioGenerationJob::id) ?: 0L) + 1L
        jobs.removeAll { it.id == resolvedId }
        jobs += job.copy(id = resolvedId)
        return resolvedId
    }

    override suspend fun update(job: AudioGenerationJob) {
        jobs.removeAll { it.id == job.id }
        jobs += job
    }

    override suspend fun get(jobId: Long): AudioGenerationJob? =
        jobs.firstOrNull { it.id == jobId }

    override suspend fun getAll(): List<AudioGenerationJob> =
        jobs.sortedByDescending(AudioGenerationJob::updatedAt)

    override suspend fun updateProgress(
        jobId: Long,
        completedDelta: Int,
        failedDelta: Int,
        status: String?,
        lastError: String?,
    ) {
        val current = get(jobId) ?: return
        update(
            current.copy(
                completedCount = current.completedCount + completedDelta,
                failedCount = current.failedCount + failedDelta,
                status = status ?: current.status,
                lastError = lastError ?: current.lastError,
                updatedAt = Instant.parse("2026-03-24T10:05:00Z"),
            ),
        )
    }
}

private class FakeAudioGenerationController(
    private val repository: AudioGenerationRepository,
) : AudioGenerationController {
    override suspend fun enqueue(
        scope: AudioGenerationScope,
        jobType: String,
        totalCount: Int,
    ): Long =
        repository.insert(
            AudioGenerationJob(
                jobType = jobType,
                sourceType = "dictionary_cache",
                scopeType = scope.scopeType,
                scopeRef = scope.scopeRef,
                status = AudioGenerationJobStatus.QUEUED.storageValue,
                totalCount = totalCount,
                createdAt = Instant.parse("2026-03-24T10:00:00Z"),
                updatedAt = Instant.parse("2026-03-24T10:00:00Z"),
            ),
        )

    override suspend fun pause(jobId: Long) {
        repository.get(jobId)?.let { job ->
            repository.update(job.copy(status = AudioGenerationJobStatus.PAUSED.storageValue))
        }
    }

    override suspend fun resume(jobId: Long) {
        repository.get(jobId)?.let { job ->
            repository.update(job.copy(status = AudioGenerationJobStatus.QUEUED.storageValue))
        }
    }

    override suspend fun cancel(jobId: Long) {
        repository.get(jobId)?.let { job ->
            repository.update(job.copy(status = AudioGenerationJobStatus.CANCELLED.storageValue))
        }
    }
}

private class FakeAudioTaskSettingsRepository(
    initial: AppSettings = AppSettings(activeBookId = "cet4"),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state
    override suspend fun getSettings(): AppSettings = state.value
    override suspend fun updateDailyGoal(dailyGoal: Int) = Unit
    override suspend fun updateActiveBookId(bookId: String?) {
        state.value = state.value.copy(activeBookId = bookId)
    }
    override suspend fun updateAiEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiBaseUrl(baseUrl: String) = Unit
    override suspend fun updateAiModel(model: String) = Unit
    override suspend fun updateDefaultAiProfileId(profileId: String?) = Unit
    override suspend fun updateWordHelpProfileId(profileId: String?) = Unit
    override suspend fun updatePlanAdjustmentProfileId(profileId: String?) = Unit
    override suspend fun updatePhoneticFillProfileId(profileId: String?) = Unit
    override suspend fun updateAiPlanAdjustmentEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiSessionCheckpointEnabled(enabled: Boolean) = Unit
    override suspend fun updatePreferredPronunciationAccent(accent: String) = Unit
    override suspend fun updatePronunciationMode(mode: String) = Unit
    override suspend fun updateAllowCellularVoicePackDownload(enabled: Boolean) = Unit
    override suspend fun updateAutoCacheWordAudio(enabled: Boolean) = Unit
    override suspend fun updateAudioCacheLimitMb(limitMb: Int) = Unit
    override suspend fun updateActiveVoicePackId(voicePackId: String?) = Unit
    override suspend fun updateFallbackToSystemTts(enabled: Boolean) = Unit
    override suspend fun updatePreferOfflineForLongText(enabled: Boolean) = Unit
    override suspend fun updateReminderEnabled(enabled: Boolean) = Unit
    override suspend fun updateReminderTime(hour: Int, minute: Int) = Unit
}

private class FakeAudioTaskBookRepository : BookRepository {
    private val books = listOf(Book(id = "cet4", title = "四级", wordCount = 2))
    private val words = listOf(
        Word(id = 1L, lemma = "abandon"),
        Word(id = 2L, lemma = "ability"),
    )

    override fun observeBooks(): Flow<List<Book>> = flowOf(books)
    override fun observeWords(bookId: String): Flow<List<Word>> = flowOf(words)
    override suspend fun getBook(bookId: String): Book? = books.firstOrNull { it.id == bookId }
    override suspend fun getAllBooks(): List<Book> = books
    override suspend fun getWords(bookId: String): List<Word> = words
    override suspend fun countWords(bookId: String): Int = words.size
    override suspend fun upsertBook(book: Book) = Unit
    override suspend fun addWordToBook(
        bookId: String,
        wordId: Long,
        chapter: String?,
        sortOrder: Int,
        tags: List<String>,
        note: String?,
    ) = Unit
    override suspend fun clearBookWordLinks(bookId: String) = Unit
    override fun loadBuiltInCatalog(inputStream: InputStream) = emptyList<com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem>()
    override suspend fun importBook(book: ImportedBook, wordIds: List<Long>): Book = books.first()
}

private class FakeAudioTaskWordRepository : WordRepository {
    private val words = listOf(
        Word(id = 1L, lemma = "abandon"),
        Word(id = 2L, lemma = "ability"),
    )

    override fun observeWords(query: String): Flow<List<Word>> = flowOf(words)
    override suspend fun getWord(wordId: Long): Word? = words.firstOrNull { it.id == wordId }
    override suspend fun getWords(wordIds: List<Long>): List<Word> = words.filter { it.id in wordIds.toSet() }
    override suspend fun getAllWords(): List<Word> = words
    override suspend fun insertWord(word: Word): Long = word.id
    override suspend fun updateWord(word: Word) = Unit
    override suspend fun importWords(words: List<ImportedWord>): List<Long> = emptyList()
}
