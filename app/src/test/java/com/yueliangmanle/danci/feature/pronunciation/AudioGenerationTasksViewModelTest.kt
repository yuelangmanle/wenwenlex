package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AudioGenerationRepository
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.importer.ImportedBook
import com.yueliangmanle.danci.core.model.AudioGenerationTask
import com.yueliangmanle.danci.core.model.AudioGenerationTaskItem
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationCoordinator
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationWorkScheduler
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioGenerationTasksViewModelTest {
    @Test
    fun loadUiState_mapsSourceNameAndProgress() = runTest {
        val viewModel = AudioGenerationTasksViewModel(
            audioGenerationRepository = FakeAudioGenerationRepository(
                mutableListOf(
                    AudioGenerationTask(
                        id = "task-1",
                        sourceId = "native-us",
                        scopeType = "book",
                        scopeRef = "cet4",
                        status = "running",
                        totalItems = 120,
                        completedItems = 48,
                        failedItems = 3,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                    ),
                ),
            ),
            pronunciationSourceRepository = FakeTaskSourceRepository(
                listOf(
                    PronunciationSource(
                        id = "native-us",
                        name = "美式原生离线包",
                        sourceType = "local_native",
                        accent = "us",
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                    ),
                ),
            ),
            audioGenerationCoordinator = buildTestCoordinator(
                repository = FakeAudioGenerationRepository(),
                sourceRepository = FakeTaskSourceRepository(emptyList()),
            ),
        )

        val state = viewModel.loadUiState()

        assertEquals(1, state.tasks.size)
        assertEquals("美式原生离线包", state.tasks.single().title)
        assertTrue(state.tasks.single().progressSummary.contains("完成 48"))
        assertTrue(state.tasks.single().progressSummary.contains("失败 3"))
    }

    @Test
    fun retryFailedItems_requeuesFailedItemsAndRefreshesUiState() = runTest {
        val repository = FakeAudioGenerationRepository(
            mutableListOf(
                AudioGenerationTask(
                    id = "task-1",
                    sourceId = "native-us",
                    scopeType = "book",
                    scopeRef = "cet4",
                    status = "failed",
                    totalItems = 4,
                    completedItems = 2,
                    failedItems = 2,
                    items = listOf(
                        AudioGenerationTaskItem(
                            taskId = "task-1",
                            itemKey = "1",
                            text = "abandon",
                            status = "completed",
                        ),
                        AudioGenerationTaskItem(
                            taskId = "task-1",
                            itemKey = "2",
                            text = "ability",
                            status = "failed",
                            failureReason = "timeout",
                            attemptCount = 1,
                        ),
                        AudioGenerationTaskItem(
                            taskId = "task-1",
                            itemKey = "3",
                            text = "able",
                            status = "failed",
                            failureReason = "provider_502",
                            attemptCount = 2,
                        ),
                    ),
                    createdAt = Instant.EPOCH,
                    updatedAt = Instant.EPOCH,
                ),
            ),
        )
        val viewModel = AudioGenerationTasksViewModel(
            audioGenerationRepository = repository,
            pronunciationSourceRepository = FakeTaskSourceRepository(
                listOf(
                    PronunciationSource(
                        id = "native-us",
                        name = "美式原生离线包",
                        sourceType = "local_native",
                        accent = "us",
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                    ),
                ),
            ),
            audioGenerationCoordinator = buildTestCoordinator(
                repository = repository,
                sourceRepository = FakeTaskSourceRepository(
                    listOf(
                        PronunciationSource(
                            id = "native-us",
                            name = "美式原生离线包",
                            sourceType = "local_native",
                            accent = "us",
                            createdAt = Instant.EPOCH,
                            updatedAt = Instant.EPOCH,
                        ),
                    ),
                ),
            ),
        )

        val state = viewModel.retryFailedItems("task-1")

        assertEquals("已重新排队 2 个失败任务。", state.statusMessage)
        assertEquals("queued", repository.tasks.single().status)
        assertEquals(0, repository.tasks.single().failedItems)
        assertEquals(2, repository.tasks.single().items.count { it.status == "queued" })
        assertTrue(state.tasks.single().progressSummary.contains("失败 0"))
        assertTrue(state.tasks.single().canRetryFailedItems.not())
    }
}

private class FakeAudioGenerationRepository(
    val tasks: MutableList<AudioGenerationTask> = mutableListOf(),
) : AudioGenerationRepository {
    override suspend fun getAllTasks(): List<AudioGenerationTask> = tasks.toList()

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

private fun buildTestCoordinator(
    repository: FakeAudioGenerationRepository,
    sourceRepository: FakeTaskSourceRepository,
): AudioGenerationCoordinator =
    AudioGenerationCoordinator(
        audioGenerationRepository = repository,
        pronunciationSourceRepository = sourceRepository,
        wordRepository = object : WordRepository {
            override fun observeWords(query: String): Flow<List<Word>> = emptyFlow()
            override suspend fun getWord(wordId: Long): Word? = null
            override suspend fun getWords(wordIds: List<Long>): List<Word> = emptyList()
            override suspend fun getAllWords(): List<Word> = emptyList()
            override suspend fun insertWord(word: Word): Long = word.id
            override suspend fun updateWord(word: Word) = Unit
            override suspend fun importWords(words: List<com.yueliangmanle.danci.core.importer.ImportedWord>): List<Long> =
                emptyList()
        },
        bookRepository = object : BookRepository {
            override fun observeBooks(): Flow<List<Book>> = emptyFlow()
            override fun observeWords(bookId: String): Flow<List<Word>> = emptyFlow()
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

            override fun loadBuiltInCatalog(inputStream: java.io.InputStream): List<com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem> =
                emptyList()

            override suspend fun importBook(book: ImportedBook, wordIds: List<Long>): Book =
                Book(id = book.metadata.id, title = book.metadata.title)
        },
        scheduler = object : AudioGenerationWorkScheduler {
            override suspend fun enqueue(taskId: String, batchSize: Int, requiresNetwork: Boolean) = Unit
        },
        idGenerator = { "test-task" },
        nowProvider = { Instant.EPOCH },
    )

private class FakeTaskSourceRepository(
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
