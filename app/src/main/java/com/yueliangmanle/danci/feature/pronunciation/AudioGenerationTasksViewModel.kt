package com.yueliangmanle.danci.feature.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.AudioGenerationRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.buildAudioGenerationRepository
import com.yueliangmanle.danci.core.data.buildPronunciationSourceRepository
import com.yueliangmanle.danci.core.model.AudioGenerationTask
import com.yueliangmanle.danci.core.model.AudioGenerationTaskItem
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationCoordinator
import com.yueliangmanle.danci.core.pronunciation.buildAudioGenerationCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioGenerationTasksUiState(
    val isLoading: Boolean = false,
    val tasks: List<AudioGenerationTaskItemUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class AudioGenerationTaskItemUiState(
    val id: String,
    val title: String,
    val subtitle: String,
    val progressSummary: String,
    val failureSummary: String? = null,
    val canRetryFailedItems: Boolean = false,
)

class AudioGenerationTasksViewModel(
    private val audioGenerationRepository: AudioGenerationRepository,
    private val pronunciationSourceRepository: PronunciationSourceRepository,
    private val audioGenerationCoordinator: AudioGenerationCoordinator,
) {
    suspend fun loadUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): AudioGenerationTasksUiState = withContext(Dispatchers.IO) {
        val sourcesById = pronunciationSourceRepository.getAllSources().associateBy { it.id }
        val tasks = audioGenerationRepository.getAllTasks().map { task ->
            buildAudioGenerationTaskItemUiState(
                task = task,
                sourceName = sourcesById[task.sourceId]?.name ?: task.sourceId,
            )
        }
        AudioGenerationTasksUiState(
            tasks = tasks,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun retryFailedItems(
        taskId: String,
    ): AudioGenerationTasksUiState = withContext(Dispatchers.IO) {
        val task = audioGenerationRepository.getTask(taskId)
            ?: return@withContext loadUiState(errorMessage = "没有找到要重试的任务。")
        val retriedCount = audioGenerationCoordinator.retryFailedItems(taskId)
        if (retriedCount <= 0) {
            return@withContext loadUiState(statusMessage = "这个任务当前没有失败项可重试。")
        }
        loadUiState(statusMessage = "已重新排队 $retriedCount 个失败任务。")
    }
}

private fun buildAudioGenerationTaskItemUiState(
    task: AudioGenerationTask,
    sourceName: String,
): AudioGenerationTaskItemUiState =
    task.items
        .filter { it.status == TASK_ITEM_STATUS_FAILED }
        .let { failedItems ->
            AudioGenerationTaskItemUiState(
                id = task.id,
                title = sourceName,
                subtitle = "${task.scopeType} · ${task.status}",
                progressSummary = "总计 ${task.totalItems} 项，完成 ${task.completedItems}，失败 ${task.failedItems}",
                failureSummary = buildFailureSummary(failedItems),
                canRetryFailedItems = failedItems.isNotEmpty(),
            )
        }

private fun buildFailureSummary(
    failedItems: List<AudioGenerationTaskItem>,
): String? {
    if (failedItems.isEmpty()) {
        return null
    }
    val firstReason = failedItems.firstNotNullOfOrNull(AudioGenerationTaskItem::failureReason)
        ?: "等待重新执行"
    return "失败项 ${failedItems.size} 个，最近原因：$firstReason"
}

suspend fun loadAudioGenerationTasksViewModel(context: Context): AudioGenerationTasksViewModel =
    withContext(Dispatchers.IO) {
        AudioGenerationTasksViewModel(
            audioGenerationRepository = buildAudioGenerationRepository(context.applicationContext),
            pronunciationSourceRepository = buildPronunciationSourceRepository(context.applicationContext),
            audioGenerationCoordinator = buildAudioGenerationCoordinator(context.applicationContext),
        )
    }

private const val TASK_ITEM_STATUS_FAILED = "failed"
