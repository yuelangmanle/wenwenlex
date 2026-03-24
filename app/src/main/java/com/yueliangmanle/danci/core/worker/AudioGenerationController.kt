package com.yueliangmanle.danci.core.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.yueliangmanle.danci.core.data.AudioGenerationRepository
import com.yueliangmanle.danci.core.data.buildAudioGenerationRepository
import com.yueliangmanle.danci.core.model.AudioGenerationJob
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobStatus
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobType
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationScope
import java.time.Instant

interface AudioGenerationController {
    suspend fun enqueue(
        scope: AudioGenerationScope,
        jobType: String,
        totalCount: Int,
    ): Long

    suspend fun pause(jobId: Long)

    suspend fun resume(jobId: Long)

    suspend fun cancel(jobId: Long)
}

class AudioGenerationScheduler(
    context: Context,
    private val repository: AudioGenerationRepository = buildAudioGenerationRepository(context.applicationContext),
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
    private val nowProvider: () -> Instant = { Instant.now() },
) : AudioGenerationController {
    override suspend fun enqueue(
        scope: AudioGenerationScope,
        jobType: String,
        totalCount: Int,
    ): Long {
        val resolvedType = AudioGenerationJobType.fromStorageValue(jobType)
        val now = nowProvider()
        val jobId = repository.insert(
            AudioGenerationJob(
                jobType = resolvedType.storageValue,
                sourceType = resolvedType.sourceType,
                scopeType = scope.scopeType,
                scopeRef = scope.scopeRef,
                status = AudioGenerationJobStatus.QUEUED.storageValue,
                totalCount = totalCount,
                createdAt = now,
                updatedAt = now,
            ),
        )
        workManager.enqueueUniqueWork(
            audioGenerationWorkName(jobId),
            ExistingWorkPolicy.REPLACE,
            buildAudioGenerationWorkRequest(jobId, resolvedType),
        )
        return jobId
    }

    override suspend fun pause(jobId: Long) {
        workManager.cancelUniqueWork(audioGenerationWorkName(jobId))
        repository.get(jobId)?.let { job ->
            repository.update(
                job.copy(
                    status = AudioGenerationJobStatus.PAUSED.storageValue,
                    updatedAt = nowProvider(),
                ),
            )
        }
    }

    override suspend fun resume(jobId: Long) {
        val job = repository.get(jobId) ?: return
        if (AudioGenerationJobStatus.fromStorageValue(job.status) == AudioGenerationJobStatus.CANCELLED) {
            return
        }
        val updatedJob = job.copy(
            status = AudioGenerationJobStatus.QUEUED.storageValue,
            updatedAt = nowProvider(),
            lastError = null,
        )
        repository.update(updatedJob)
        workManager.enqueueUniqueWork(
            audioGenerationWorkName(jobId),
            ExistingWorkPolicy.REPLACE,
            buildAudioGenerationWorkRequest(jobId, AudioGenerationJobType.fromStorageValue(job.jobType)),
        )
    }

    override suspend fun cancel(jobId: Long) {
        workManager.cancelUniqueWork(audioGenerationWorkName(jobId))
        repository.get(jobId)?.let { job ->
            repository.update(
                job.copy(
                    status = AudioGenerationJobStatus.CANCELLED.storageValue,
                    updatedAt = nowProvider(),
                ),
            )
        }
    }
}

fun buildAudioGenerationController(context: Context): AudioGenerationController =
    AudioGenerationScheduler(context.applicationContext)
