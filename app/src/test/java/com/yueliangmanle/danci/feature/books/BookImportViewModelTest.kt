package com.yueliangmanle.danci.feature.books

import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.ai.AiRuntimeSettings
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.ImportBatchRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.importer.ImportRepairCoordinator
import com.yueliangmanle.danci.core.importer.ImportPreview
import com.yueliangmanle.danci.core.importer.ImportPreviewRow
import com.yueliangmanle.danci.core.importer.ImportedWord
import com.yueliangmanle.danci.core.importer.XlsxSheetData
import com.yueliangmanle.danci.core.model.AiCapability
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.ImportBatch
import com.yueliangmanle.danci.core.model.Word
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookImportViewModelTest {
    @Test
    fun loadFromBytes_showsDiagnosisSummaryAndAiRepairEntryForSevereIssues() = runTest {
        val sheets = listOf(
            XlsxSheetData(
                name = "Sheet1",
                rows = listOf(
                    listOf("", "abandon", "放弃"),
                ),
            ),
        )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = BookImportViewModel(
            context = context,
            wordRepository = FakeWordRepository(),
            bookRepository = FakeBookRepository(),
            importBatchRepository = FakeImportBatchRepository(),
            workbookLoader = { sheets },
            repairCoordinator = ImportRepairCoordinator(
                aiNormalize = { _, _ -> error("should not call ai when loading") },
            ),
        )

        val state = viewModel.loadFromBytes(
            fileName = "broken.xlsx",
            bytes = byteArrayOf(0x01),
        )

        assertEquals(0, state.minorIssueCount)
        assertEquals(1, state.severeIssueCount)
        assertTrue(state.canRunAiRepair)
        assertTrue(state.diagnosisSummary.orEmpty().contains("高风险"))
    }

    @Test
    fun normalizeWithAi_updatesPreviewAfterExplicitRepair() = runTest {
        val sheets = listOf(
            XlsxSheetData(
                name = "Sheet1",
                rows = listOf(
                    listOf("", "abandon", "放弃"),
                ),
            ),
        )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = BookImportViewModel(
            context = context,
            wordRepository = FakeWordRepository(),
            bookRepository = FakeBookRepository(),
            importBatchRepository = FakeImportBatchRepository(),
            workbookLoader = { sheets },
            runtimeSettingsResolver = { _, capability ->
                assertEquals(AiCapability.WORD_HELP, capability)
                AiRuntimeSettings(
                    enabled = true,
                    baseUrl = "https://example.com/v1",
                    apiKey = "test-key",
                    model = "test-model",
                )
            },
            repairCoordinator = ImportRepairCoordinator(
                aiNormalize = { _, _ ->
                    ImportPreview(
                        sheetName = "Sheet1",
                        rows = listOf(
                            ImportPreviewRow(
                                word = "abandon",
                                meanings = listOf("放弃"),
                                sourceLabel = "AI 修复",
                            ),
                        ),
                        totalRows = 1,
                        skippedRows = 0,
                        parserMode = "ai_repaired",
                        aiNormalizedCount = 1,
                        aiCompletedCount = 1,
                    )
                },
            ),
        )

        val loaded = viewModel.loadFromBytes(
            fileName = "broken.xlsx",
            bytes = byteArrayOf(0x01),
        )
        val state = viewModel.normalizeWithAi(loaded)

        assertEquals("AI 修复", state.preview?.rows?.first()?.sourceLabel)
        assertTrue(state.statusMessage.orEmpty().contains("AI"))
        assertTrue(!state.canRunAiRepair)
    }
}

private class FakeWordRepository : WordRepository {
    override fun observeWords(query: String): Flow<List<Word>> = flowOf(emptyList())

    override suspend fun getWord(wordId: Long): Word? = null

    override suspend fun getWords(wordIds: List<Long>): List<Word> = emptyList()

    override suspend fun getAllWords(): List<Word> = emptyList()

    override suspend fun insertWord(word: Word): Long = 1L

    override suspend fun updateWord(word: Word) = Unit

    override suspend fun importWords(words: List<ImportedWord>): List<Long> =
        words.indices.map { index -> index.toLong() + 1L }
}

private class FakeBookRepository : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = flowOf(emptyList())

    override fun observeWords(bookId: String): Flow<List<Word>> = flowOf(emptyList())

    override suspend fun getBook(bookId: String): Book? = null

    override suspend fun getAllBooks(): List<Book> = emptyList()

    override suspend fun getWords(bookId: String): List<Word> = emptyList()

    override suspend fun countWords(bookId: String): Int = 0

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

    override fun loadBuiltInCatalog(inputStream: java.io.InputStream) = emptyList<com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem>()

    override suspend fun importBook(
        book: com.yueliangmanle.danci.core.importer.ImportedBook,
        wordIds: List<Long>,
    ): Book =
        Book(
            id = "imported-book",
            title = book.metadata.title,
            description = book.metadata.description,
            wordCount = wordIds.size,
            sourceType = "imported",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
}

private class FakeImportBatchRepository : ImportBatchRepository {
    val inserted = mutableListOf<ImportBatch>()

    override suspend fun insert(batch: ImportBatch): Long {
        inserted += batch
        return inserted.size.toLong()
    }

    override suspend fun getAll(): List<ImportBatch> = inserted.toList()
}
