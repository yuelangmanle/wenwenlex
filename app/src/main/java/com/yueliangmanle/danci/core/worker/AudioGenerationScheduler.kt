package com.yueliangmanle.danci.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationWorkScheduler
import java.util.concurrent.TimeUnit

internal const val INPUT_TASK_ID = "task_id"
internal const val INPUT_BATCH_SIZE = "batch_size"

const val AUDIO_GENERATION_WORK_PREFIX = "audio_generation_"

class AudioGenerationScheduler(
    context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
) : AudioGenerationWorkScheduler {
    override suspend fun enqueue(
        taskId: String,
        batchSize: Int,
        requiresNetwork: Boolean,
    ) {
        workManager.enqueueUniqueWork(
            audioGenerationUniqueWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            buildAudioGenerationWorkRequest(
                taskId = taskId,
                batchSize = batchSize,
                requiresNetwork = requiresNetwork,
            ),
        )
    }
}

internal fun buildAudioGenerationWorkRequest(
    taskId: String,
    batchSize: Int,
    requiresNetwork: Boolean,
    delayMs: Long = 0L,
): OneTimeWorkRequest {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(
            if (requiresNetwork) {
                NetworkType.CONNECTED
            } else {
                NetworkType.NOT_REQUIRED
            },
        )
        .build()
    return OneTimeWorkRequestBuilder<AudioGenerationWorker>()
        .setConstraints(constraints)
        .setInputData(
            workDataOf(
                INPUT_TASK_ID to taskId,
                INPUT_BATCH_SIZE to batchSize.coerceAtLeast(1),
            ),
        )
        .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
        .addTag(audioGenerationUniqueWorkName(taskId))
        .build()
}

internal fun audioGenerationUniqueWorkName(taskId: String): String =
    "$AUDIO_GENERATION_WORK_PREFIX$taskId"
