package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.AudioGenerationJobDao
import com.yueliangmanle.danci.core.database.entity.AudioGenerationJobEntity
import com.yueliangmanle.danci.core.model.AudioGenerationJob
import java.time.Instant

interface AudioGenerationRepository {
    suspend fun insert(job: AudioGenerationJob): Long
    suspend fun update(job: AudioGenerationJob)
    suspend fun get(jobId: Long): AudioGenerationJob?
    suspend fun getAll(): List<AudioGenerationJob>
    suspend fun updateProgress(
        jobId: Long,
        completedDelta: Int = 0,
        failedDelta: Int = 0,
        status: String? = null,
        lastError: String? = null,
    )
}

class RoomAudioGenerationRepository(
    private val dao: AudioGenerationJobDao,
    private val nowProvider: () -> Instant = { Instant.now() },
) : AudioGenerationRepository {
    override suspend fun insert(job: AudioGenerationJob): Long =
        dao.insertJob(job.asEntity())

    override suspend fun update(job: AudioGenerationJob) {
        dao.updateJob(job.asEntity())
    }

    override suspend fun get(jobId: Long): AudioGenerationJob? =
        dao.getJobById(jobId)?.asExternalModel()

    override suspend fun getAll(): List<AudioGenerationJob> =
        dao.getAllJobs().map(AudioGenerationJobEntity::asExternalModel)

    override suspend fun updateProgress(
        jobId: Long,
        completedDelta: Int,
        failedDelta: Int,
        status: String?,
        lastError: String?,
    ) {
        val current = get(jobId) ?: return
        update(
            current.copy(
                completedCount = (current.completedCount + completedDelta).coerceAtLeast(0),
                failedCount = (current.failedCount + failedDelta).coerceAtLeast(0),
                status = status ?: current.status,
                lastError = lastError ?: current.lastError,
                updatedAt = nowProvider(),
            ),
        )
    }
}

internal fun AudioGenerationJob.asEntity(): AudioGenerationJobEntity =
    AudioGenerationJobEntity(
        id = id,
        jobType = jobType,
        sourceType = sourceType,
        scopeType = scopeType,
        scopeRef = scopeRef,
        status = status,
        totalCount = totalCount,
        completedCount = completedCount,
        failedCount = failedCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastError = lastError,
    )

internal fun AudioGenerationJobEntity.asExternalModel(): AudioGenerationJob =
    AudioGenerationJob(
        id = id,
        jobType = jobType,
        sourceType = sourceType,
        scopeType = scopeType,
        scopeRef = scopeRef,
        status = status,
        totalCount = totalCount,
        completedCount = completedCount,
        failedCount = failedCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastError = lastError,
    )

fun buildAudioGenerationRepository(context: Context): AudioGenerationRepository =
    RoomAudioGenerationRepository(
        dao = buildDanciDatabase(context.applicationContext).audioGenerationJobDao(),
    )
