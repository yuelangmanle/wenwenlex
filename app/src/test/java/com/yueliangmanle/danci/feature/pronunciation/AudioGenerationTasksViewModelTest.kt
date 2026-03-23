package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AudioGenerationRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.model.AudioGenerationTask
import com.yueliangmanle.danci.core.model.AudioGenerationTaskItem
import com.yueliangmanle.danci.core.model.PronunciationSource
import java.time.Instant
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
    val tasks: MutableList<AudioGenerationTask>,
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
