package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.AudioGenerationTaskDao
import com.yueliangmanle.danci.core.database.entity.AudioGenerationTaskEntity
import com.yueliangmanle.danci.core.database.entity.AudioGenerationTaskItemEntity
import com.yueliangmanle.danci.core.model.AudioGenerationTask
import com.yueliangmanle.danci.core.model.AudioGenerationTaskItem

interface AudioGenerationRepository {
    suspend fun getAllTasks(): List<AudioGenerationTask>
    suspend fun upsertTasks(tasks: List<AudioGenerationTask>)
    suspend fun clearAll()
}

class RoomAudioGenerationRepository(
    private val dao: AudioGenerationTaskDao,
) : AudioGenerationRepository {
    override suspend fun getAllTasks(): List<AudioGenerationTask> =
        dao.getAllTasks().map { task ->
            task.asExternalModel(
                items = dao.getItemsByTask(task.id).map(AudioGenerationTaskItemEntity::asExternalModel),
            )
        }

    override suspend fun upsertTasks(tasks: List<AudioGenerationTask>) {
        if (tasks.isEmpty()) {
            return
        }
        dao.upsertTasks(tasks.map(AudioGenerationTask::asEntity))
        val allItems = tasks.flatMap { task ->
            task.items.map { item -> item.asEntity(taskId = task.id) }
        }
        if (allItems.isNotEmpty()) {
            dao.upsertTaskItems(allItems)
        }
    }

    override suspend fun clearAll() {
        dao.clearTaskItems()
        dao.clearTasks()
    }
}

internal fun AudioGenerationTask.asEntity(): AudioGenerationTaskEntity =
    AudioGenerationTaskEntity(
        id = id,
        sourceId = sourceId,
        presetId = presetId,
        scopeType = scopeType,
        scopeRef = scopeRef,
        status = status,
        totalItems = totalItems,
        completedItems = completedItems,
        failedItems = failedItems,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun AudioGenerationTaskEntity.asExternalModel(
    items: List<AudioGenerationTaskItem>,
): AudioGenerationTask =
    AudioGenerationTask(
        id = id,
        sourceId = sourceId,
        presetId = presetId,
        scopeType = scopeType,
        scopeRef = scopeRef,
        status = status,
        totalItems = totalItems,
        completedItems = completedItems,
        failedItems = failedItems,
        items = items,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun AudioGenerationTaskItem.asEntity(taskId: String): AudioGenerationTaskItemEntity =
    AudioGenerationTaskItemEntity(
        taskId = taskId,
        itemKey = itemKey,
        wordId = wordId,
        text = text,
        status = status,
        failureReason = failureReason,
        attemptCount = attemptCount,
        generatedAssetId = generatedAssetId,
    )

internal fun AudioGenerationTaskItemEntity.asExternalModel(): AudioGenerationTaskItem =
    AudioGenerationTaskItem(
        taskId = taskId,
        itemKey = itemKey,
        wordId = wordId,
        text = text,
        status = status,
        failureReason = failureReason,
        attemptCount = attemptCount,
        generatedAssetId = generatedAssetId,
    )

fun buildAudioGenerationRepository(context: Context): AudioGenerationRepository =
    RoomAudioGenerationRepository(buildDanciDatabase(context.applicationContext).audioGenerationTaskDao())
