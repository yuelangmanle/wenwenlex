package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.PhoneticEnrichmentJobDao
import com.yueliangmanle.danci.core.database.entity.PhoneticEnrichmentJobEntity
import com.yueliangmanle.danci.core.model.PhoneticEnrichmentJob

interface PhoneticEnrichmentRepository {
    suspend fun insert(job: PhoneticEnrichmentJob): Long
    suspend fun update(job: PhoneticEnrichmentJob)
    suspend fun getJob(jobId: Long): PhoneticEnrichmentJob?
}

class RoomPhoneticEnrichmentRepository(
    private val dao: PhoneticEnrichmentJobDao,
) : PhoneticEnrichmentRepository {
    override suspend fun insert(job: PhoneticEnrichmentJob): Long = dao.insertJob(job.asEntity())

    override suspend fun update(job: PhoneticEnrichmentJob) {
        dao.updateJob(job.asEntity())
    }

    override suspend fun getJob(jobId: Long): PhoneticEnrichmentJob? =
        dao.getJobById(jobId)?.asExternalModel()
}

internal fun PhoneticEnrichmentJob.asEntity(): PhoneticEnrichmentJobEntity =
    PhoneticEnrichmentJobEntity(
        id = id,
        scopeType = scopeType,
        scopeRef = scopeRef,
        profileId = profileId,
        fillMode = fillMode,
        status = status,
        totalCount = totalCount,
        completedCount = completedCount,
        failedCount = failedCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun PhoneticEnrichmentJobEntity.asExternalModel(): PhoneticEnrichmentJob =
    PhoneticEnrichmentJob(
        id = id,
        scopeType = scopeType,
        scopeRef = scopeRef,
        profileId = profileId,
        fillMode = fillMode,
        status = status,
        totalCount = totalCount,
        completedCount = completedCount,
        failedCount = failedCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun buildPhoneticEnrichmentRepository(context: Context): PhoneticEnrichmentRepository =
    RoomPhoneticEnrichmentRepository(
        buildDanciDatabase(context.applicationContext).phoneticEnrichmentJobDao(),
    )
