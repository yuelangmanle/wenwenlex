package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.data.AudioGenerationRepository
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.importer.ImportedBook
import com.yueliangmanle.danci.core.model.AudioGenerationTask
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourcePreset
import com.yueliangmanle.danci.core.model.Word
import java.io.InputStream
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioGenerationCoordinatorTest {
    @Test
    fun enqueueBookGeneration_splitsIntoBatchesAndPersistsItems() = runTest {
        val repository = FakeCoordinatorAudioGenerationRepository()
        val scheduler = FakeAudioGenerationWorkScheduler()
        val coordinator = AudioGenerationCoordinator(
            audioGenerationRepository = repository,
            pronunciationSourceRepository = FakeCoordinatorSourceRepository(
                listOf(
                    coordinatorSource(
                        id = "cloud-mimo",
                        sourceType = "cloud_tts",
                        providerProfileId = "profile-mimo",
                        presets = listOf(
                            PronunciationSourcePreset(
                                sourceId = "cloud-mimo",
                                presetId = "preset-calm",
                                displayName = "平静讲解",
                                voice = "default_en",
                                styleTemplate = "Calm",
                                isDefaultPreset = true,
                            ),
                        ),
                    ),
                ),
            ),
            wordRepository = FakeCoordinatorWordRepository(),
            bookRepository = FakeCoordinatorBookRepository(
                wordsByBookId = mapOf(
                    "cet4" to (1..120).map { index ->
                        Word(
                            id = index.toLong(),
                            lemma = "word$index",
                        )
                    },
                ),
            ),
            scheduler = scheduler,
            idGenerator = { "task-book-1" },
            nowProvider = { Instant.parse("2026-03-23T12:00:00Z") },
        )

        val taskId = coordinator.enqueueBook(
            sourceId = "cloud-mimo",
            bookId = "cet4",
            presetId = null,
            batchSize = 100,
            runInBackground = true,
        )

        val task = requireNotNull(repository.getTask(taskId))
        assertEquals("book", task.scopeType)
        assertEquals("cet4", task.scopeRef)
        assertEquals("preset-calm", task.presetId)
        assertEquals(120, task.totalItems)
        assertEquals(120, task.items.size)
        assertEquals(1, scheduler.enqueuedRequests.size)
        assertEquals("task-book-1", scheduler.enqueuedRequests.single().taskId)
        assertEquals(100, scheduler.enqueuedRequests.single().batchSize)
        assertTrue(scheduler.enqueuedRequests.single().requiresNetwork)
    }

    @Test
    fun retryFailedTask_resetsFailedItemsToPendingAndRequeuesWorker() = runTest {
        val repository = FakeCoordinatorAudioGenerationRepository(
            mutableListOf(
                AudioGenerationTask(
                    id = "task-1",
                    sourceId = "native-us",
                    presetId = null,
                    scopeType = "book",
                    scopeRef = "cet4",
                    status = "failed",
                    totalItems = 3,
                    completedItems = 1,
                    failedItems = 2,
                    items = listOf(
                        com.yueliangmanle.danci.core.model.AudioGenerationTaskItem(
                            taskId = "task-1",
                            itemKey = "1",
                            wordId = 1L,
                            text = "abandon",
                            status = "completed",
                        ),
                        com.yueliangmanle.danci.core.model.AudioGenerationTaskItem(
                            taskId = "task-1",
                            itemKey = "2",
                            wordId = 2L,
                            text = "ability",
                            status = "failed",
                            failureReason = "timeout",
                            attemptCount = 1,
                        ),
                        com.yueliangmanle.danci.core.model.AudioGenerationTaskItem(
                            taskId = "task-1",
                            itemKey = "3",
                            wordId = 3L,
                            text = "able",
                            status = "failed",
                            failureReason = "unsupported",
                            attemptCount = 2,
                        ),
                    ),
                    createdAt = Instant.EPOCH,
                    updatedAt = Instant.EPOCH,
                ),
            ),
        )
        val scheduler = FakeAudioGenerationWorkScheduler()
        val coordinator = AudioGenerationCoordinator(
            audioGenerationRepository = repository,
            pronunciationSourceRepository = FakeCoordinatorSourceRepository(
                listOf(
                    coordinatorSource(
                        id = "native-us",
                        sourceType = "local_native",
                        backingVoicePackId = "native-us",
                    ),
                ),
            ),
            wordRepository = FakeCoordinatorWordRepository(),
            bookRepository = FakeCoordinatorBookRepository(),
            scheduler = scheduler,
            idGenerator = { "unused" },
            nowProvider = { Instant.parse("2026-03-23T12:05:00Z") },
        )

        val retriedCount = coordinator.retryFailedItems("task-1")

        val task = requireNotNull(repository.getTask("task-1"))
        assertEquals(2, retriedCount)
        assertEquals("queued", task.status)
        assertEquals(0, task.failedItems)
        assertEquals(2, task.items.count { it.status == "queued" })
        assertEquals(1, scheduler.enqueuedRequests.size)
        assertEquals("task-1", scheduler.enqueuedRequests.single().taskId)
        assertTrue(scheduler.enqueuedRequests.single().requiresNetwork.not())
    }
}

private fun coordinatorSource(
    id: String,
    sourceType: String,
    providerProfileId: String? = null,
    backingVoicePackId: String? = null,
    presets: List<PronunciationSourcePreset> = emptyList(),
): PronunciationSource =
    PronunciationSource(
        id = id,
        name = id,
        sourceType = sourceType,
        accent = "us",
        providerProfileId = providerProfileId,
        backingVoicePackId = backingVoicePackId,
        presets = presets,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

private class FakeCoordinatorAudioGenerationRepository(
    private val tasks: MutableList<AudioGenerationTask> = mutableListOf(),
) : AudioGenerationRepository {
    override suspend fun getAllTasks(): List<AudioGenerationTask> = tasks.toList()

    override suspend fun getTask(taskId: String): AudioGenerationTask? =
        tasks.firstOrNull { it.id == taskId }

    override suspend fun upsertTasks(tasks: List<AudioGenerationTask>) {
        tasks.forEach { task ->
            val index = this.tasks.indexOfFirst { it.id == task.id }
            if (index >= 0) {
                this.tasks[index] = task
            } else {
                this.tasks += task
            }
        }
    }

    override suspend fun clearAll() = Unit
}

private class FakeCoordinatorSourceRepository(
    private val sources: List<PronunciationSource>,
) : PronunciationSourceRepository {
    override suspend fun getAllSources(): List<PronunciationSource> = sources

    override suspend fun getSource(sourceId: String): PronunciationSource? =
        sources.firstOrNull { it.id == sourceId }

    override suspend fun upsertSources(sources: List<PronunciationSource>) = Unit

    override suspend fun setDefaultWordSource(sourceId: String) = Unit

    override suspend fun setDefaultLongTextSource(sourceId: String) = Unit

    override suspend fun clearAll() = Unit
}

private class FakeCoordinatorWordRepository(
    private val words: List<Word> = emptyList(),
) : WordRepository {
    override fun observeWords(query: String): Flow<List<Word>> = emptyFlow()

    override suspend fun getWord(wordId: Long): Word? =
        words.firstOrNull { it.id == wordId }

    override suspend fun getWords(wordIds: List<Long>): List<Word> =
        words.filter { it.id in wordIds }

    override suspend fun getAllWords(): List<Word> = words

    override suspend fun insertWord(word: Word): Long = word.id

    override suspend fun updateWord(word: Word) = Unit

    override suspend fun importWords(words: List<com.yueliangmanle.danci.core.importer.ImportedWord>): List<Long> =
        emptyList()
}

private class FakeCoordinatorBookRepository(
    private val wordsByBookId: Map<String, List<Word>> = emptyMap(),
) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = emptyFlow()

    override fun observeWords(bookId: String): Flow<List<Word>> = emptyFlow()

    override suspend fun getBook(bookId: String): Book? =
        wordsByBookId[bookId]?.let { words ->
            Book(
                id = bookId,
                title = bookId,
                wordCount = words.size,
            )
        }

    override suspend fun getAllBooks(): List<Book> =
        wordsByBookId.map { (bookId, words) ->
            Book(
                id = bookId,
                title = bookId,
                wordCount = words.size,
            )
        }

    override suspend fun getWords(bookId: String): List<Word> =
        wordsByBookId[bookId].orEmpty()

    override suspend fun countWords(bookId: String): Int =
        wordsByBookId[bookId]?.size ?: 0

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

    override fun loadBuiltInCatalog(inputStream: InputStream): List<com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem> =
        emptyList()

    override suspend fun importBook(book: ImportedBook, wordIds: List<Long>): Book =
        Book(
            id = book.metadata.id,
            title = book.metadata.title,
            wordCount = wordIds.size,
        )
}

private class FakeAudioGenerationWorkScheduler : AudioGenerationWorkScheduler {
    val enqueuedRequests = mutableListOf<EnqueuedGenerationRequest>()

    override suspend fun enqueue(
        taskId: String,
        batchSize: Int,
        requiresNetwork: Boolean,
    ) {
        enqueuedRequests += EnqueuedGenerationRequest(
            taskId = taskId,
            batchSize = batchSize,
            requiresNetwork = requiresNetwork,
        )
    }
}

private data class EnqueuedGenerationRequest(
    val taskId: String,
    val batchSize: Int,
    val requiresNetwork: Boolean,
)
