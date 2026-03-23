package com.yueliangmanle.danci.core.enrichment

import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.PhoneticEnrichmentRepository
import com.yueliangmanle.danci.core.importer.ImportedBook
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.PhoneticEnrichmentJob
import com.yueliangmanle.danci.core.model.Word
import java.io.InputStream
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordQualityEnrichmentCoordinatorTest {
    @Test
    fun createJob_buildsRequestedQualityDimensions() {
        val coordinator = WordQualityEnrichmentCoordinator(
            repository = FakePhoneticEnrichmentRepository(),
            bookRepository = FakeQualityBookRepository(),
            nowProvider = { Instant.parse("2026-03-23T10:00:00Z") },
        )

        val job = coordinator.createBookJob(
            bookId = "ielts-core",
            dimensions = setOf(
                QualityFillDimension.PHONETIC,
                QualityFillDimension.SYNONYM,
                QualityFillDimension.ANTONYM,
                QualityFillDimension.SIMILAR,
                QualityFillDimension.EXAMPLE,
                QualityFillDimension.WORD_FORM,
            ),
            totalCount = 12,
        )

        assertEquals(6, coordinator.parseDimensions(job.fillMode).size)
        assertTrue(job.fillMode.contains("phonetic"))
        assertTrue(job.fillMode.contains("word_form"))
    }

    @Test
    fun enqueueBookEnrichment_persistsQueuedJob() = runTest {
        val repository = FakePhoneticEnrichmentRepository()
        val coordinator = WordQualityEnrichmentCoordinator(
            repository = repository,
            bookRepository = FakeQualityBookRepository(),
            nowProvider = { Instant.parse("2026-03-23T10:00:00Z") },
        )

        val jobId = coordinator.enqueueBookEnrichment(
            bookId = "ielts-core",
            dimensions = coordinator.defaultDimensions(),
        )

        val job = repository.getJob(jobId)!!
        assertEquals("book", job.scopeType)
        assertEquals("queued", job.status)
        assertEquals(2, job.totalCount)
    }
}

private class FakePhoneticEnrichmentRepository : PhoneticEnrichmentRepository {
    private val jobs = linkedMapOf<Long, PhoneticEnrichmentJob>()
    private var nextId = 1L

    override suspend fun insert(job: PhoneticEnrichmentJob): Long {
        val id = nextId++
        jobs[id] = job.copy(id = id)
        return id
    }

    override suspend fun update(job: PhoneticEnrichmentJob) {
        jobs[job.id] = job
    }

    override suspend fun getJob(jobId: Long): PhoneticEnrichmentJob? = jobs[jobId]

    override suspend fun getLatestJob(scopeType: String, scopeRef: String): PhoneticEnrichmentJob? =
        jobs.values.lastOrNull { it.scopeType == scopeType && it.scopeRef == scopeRef }
}

private class FakeQualityBookRepository : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = flowOf(emptyList())

    override fun observeWords(bookId: String): Flow<List<Word>> = flowOf(emptyList())

    override suspend fun getBook(bookId: String): Book? =
        Book(
            id = bookId,
            title = "雅思核心词",
            wordCount = 2,
            sourceType = "builtin",
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
