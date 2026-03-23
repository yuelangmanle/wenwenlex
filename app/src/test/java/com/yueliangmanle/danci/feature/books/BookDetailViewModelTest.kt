package com.yueliangmanle.danci.feature.books

import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.ai.AiStrategyCoordinator
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.ImportBatchRepository
import com.yueliangmanle.danci.core.data.PhoneticEnrichmentRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.enrichment.WordQualityEnrichmentCoordinator
import com.yueliangmanle.danci.core.importer.ImportedBook
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.ImportBatch
import com.yueliangmanle.danci.core.model.PhoneticEnrichmentJob
import com.yueliangmanle.danci.core.model.Word
import java.io.InputStream
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailViewModelTest {
    @Test
    fun loadBookDetail_showsQualityActionsForBuiltinAndImportedBooks() = runTest {
        val repository = DetailPhoneticRepository()
        val viewModel = BookDetailViewModel(
            context = ApplicationProvider.getApplicationContext(),
            bookRepository = DetailBookRepository(),
            wordRepository = DetailWordRepository(),
            settingsRepository = DetailSettingsRepository(),
            importBatchRepository = DetailImportBatchRepository(),
            phoneticEnrichmentRepository = repository,
            coordinator = AiStrategyCoordinator(),
            qualityEnrichmentCoordinator = WordQualityEnrichmentCoordinator(
                repository = repository,
                bookRepository = DetailBookRepository(),
                nowProvider = { Instant.parse("2026-03-23T11:00:00Z") },
            ),
        )

        val state = viewModel.loadUiState(bookId = "builtin-cet4")

        assertTrue(state.canStartQualityEnrichment)
        assertTrue(state.qualityEnrichmentSummary.orEmpty().contains("音标"))
    }
}

private class DetailBookRepository : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = flowOf(emptyList())

    override fun observeWords(bookId: String): Flow<List<Word>> = flowOf(emptyList())

    override suspend fun getBook(bookId: String): Book? =
        Book(
            id = bookId,
            title = "四级核心词",
            sourceType = if (bookId.startsWith("builtin")) "builtin" else "imported",
            wordCount = 2,
        )

    override suspend fun getAllBooks(): List<Book> = emptyList()

    override suspend fun getWords(bookId: String): List<Word> =
        listOf(
            Word(id = 1L, lemma = "abandon", meanings = listOf("放弃")),
            Word(id = 2L, lemma = "brief", meanings = listOf("简短的")),
        )

    override suspend fun countWords(bookId: String): Int = 2

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

    override suspend fun importBook(book: ImportedBook, wordIds: List<Long>): Book =
        Book(id = book.metadata.id, title = book.metadata.title, wordCount = wordIds.size, sourceType = "imported")
}

private class DetailWordRepository : WordRepository {
    override fun observeWords(query: String): Flow<List<Word>> = flowOf(emptyList())

    override suspend fun getWord(wordId: Long): Word? = null

    override suspend fun getWords(wordIds: List<Long>): List<Word> = emptyList()

    override suspend fun getAllWords(): List<Word> = emptyList()

    override suspend fun insertWord(word: Word): Long = 0L

    override suspend fun updateWord(word: Word) = Unit

    override suspend fun importWords(words: List<com.yueliangmanle.danci.core.importer.ImportedWord>): List<Long> = emptyList()
}

private class DetailImportBatchRepository : ImportBatchRepository {
    override suspend fun insert(batch: ImportBatch): Long = 1L

    override suspend fun getAll(): List<ImportBatch> = emptyList()
}

private class DetailPhoneticRepository : PhoneticEnrichmentRepository {
    override suspend fun insert(job: PhoneticEnrichmentJob): Long = 1L

    override suspend fun update(job: PhoneticEnrichmentJob) = Unit

    override suspend fun getJob(jobId: Long): PhoneticEnrichmentJob? = null

    override suspend fun getLatestJob(scopeType: String, scopeRef: String): PhoneticEnrichmentJob? = null
}

private class DetailSettingsRepository : SettingsRepository {
    override val settings: Flow<com.yueliangmanle.danci.core.data.AppSettings> =
        flowOf(com.yueliangmanle.danci.core.data.AppSettings())

    override suspend fun getSettings() = com.yueliangmanle.danci.core.data.AppSettings()
    override suspend fun updateDailyGoal(value: Int) = Unit
    override suspend fun updateWeeklyGoal(value: Int) = Unit
    override suspend fun updatePhaseName(value: String?) = Unit
    override suspend fun updatePhaseTargetWords(value: Int) = Unit
    override suspend fun updateActiveBookId(value: String?) = Unit
    override suspend fun updateAiEnabled(value: Boolean) = Unit
    override suspend fun updateAiBaseUrl(value: String) = Unit
    override suspend fun updateAiModel(value: String) = Unit
    override suspend fun updateDefaultAiProfileId(value: String?) = Unit
    override suspend fun updateWordHelpProfileId(value: String?) = Unit
    override suspend fun updatePlanAdjustmentProfileId(value: String?) = Unit
    override suspend fun updatePhoneticFillProfileId(value: String?) = Unit
    override suspend fun updateAiPlanAdjustmentEnabled(value: Boolean) = Unit
    override suspend fun updateAiSessionCheckpointEnabled(value: Boolean) = Unit
    override suspend fun updatePreferredPronunciationAccent(value: String) = Unit
    override suspend fun updatePronunciationMode(value: String) = Unit
    override suspend fun updateAllowCellularVoicePackDownload(value: Boolean) = Unit
    override suspend fun updateAutoCacheWordAudio(value: Boolean) = Unit
    override suspend fun updateAudioCacheLimitMb(value: Int) = Unit
    override suspend fun updateActiveVoicePackId(value: String?) = Unit
    override suspend fun updateFallbackToSystemTts(value: Boolean) = Unit
    override suspend fun updatePreferOfflineForLongText(value: Boolean) = Unit
    override suspend fun updateReminderEnabled(enabled: Boolean) = Unit
    override suspend fun updateReminderTime(hour: Int, minute: Int) = Unit
}
