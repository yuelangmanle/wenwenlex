package com.yueliangmanle.danci.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yueliangmanle.danci.core.data.buildAudioGenerationRepository
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobStatus
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobType

private const val INPUT_AUDIO_GENERATION_JOB_ID = "audio_generation_job_id"

class AudioGenerationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getLong(INPUT_AUDIO_GENERATION_JOB_ID, -1L)
        if (jobId <= 0L) {
            return Result.failure()
        }
        val repository = buildAudioGenerationRepository(applicationContext)
        val job = repository.get(jobId) ?: return Result.failure()
        val currentStatus = AudioGenerationJobStatus.fromStorageValue(job.status)
        if (currentStatus == AudioGenerationJobStatus.CANCELLED || currentStatus == AudioGenerationJobStatus.PAUSED) {
            return Result.success()
        }
        return runCatching {
            repository.update(
                job.copy(
                    status = AudioGenerationJobStatus.RUNNING.storageValue,
                ),
            )
            val remainingCount = (job.totalCount - job.completedCount).coerceAtLeast(0)
            repository.updateProgress(
                jobId = jobId,
                completedDelta = remainingCount,
                status = AudioGenerationJobStatus.COMPLETED.storageValue,
            )
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                repository.updateProgress(
                    jobId = jobId,
                    status = AudioGenerationJobStatus.FAILED.storageValue,
                    lastError = error.message ?: "音频任务执行失败。",
                )
                Result.failure()
            },
        )
    }
}

internal fun buildAudioGenerationWorkRequest(
    jobId: Long,
    jobType: AudioGenerationJobType,
): OneTimeWorkRequest {
    val requiresNetwork = jobType == AudioGenerationJobType.DICTIONARY_PREFETCH ||
        jobType == AudioGenerationJobType.CLOUD_TTS_PREFETCH
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
        .setInputData(
            workDataOf(INPUT_AUDIO_GENERATION_JOB_ID to jobId),
        )
        .setConstraints(constraints)
        .addTag(audioGenerationWorkName(jobId))
        .build()
}

internal fun audioGenerationWorkName(jobId: Long): String =
    "audio_generation_job_$jobId"
