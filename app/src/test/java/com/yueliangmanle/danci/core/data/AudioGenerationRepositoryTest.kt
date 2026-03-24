package com.yueliangmanle.danci.core.data

import com.yueliangmanle.danci.core.database.dao.AudioGenerationJobDao
import com.yueliangmanle.danci.core.database.entity.AudioGenerationJobEntity
import com.yueliangmanle.danci.core.model.AudioGenerationJob
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobStatus
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobType
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationScope
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AudioGenerationRepositoryTest {
    @Test
    fun updateProgress_incrementsCompletedAndFailedCounts() = runTest {
        val repository = RoomAudioGenerationRepository(
            dao = FakeAudioGenerationJobDao(),
            nowProvider = { Instant.parse("2026-03-24T10:00:00Z") },
        )

        val jobId = repository.insert(
            job(
                jobType = AudioGenerationJobType.DICTIONARY_PREFETCH.storageValue,
                totalCount = 10,
            ),
        )
        repository.updateProgress(
            jobId = jobId,
            completedDelta = 3,
            failedDelta = 1,
        )

        val loaded = repository.get(jobId)
        assertNotNull(loaded)
        assertEquals(3, loaded?.completedCount)
        assertEquals(1, loaded?.failedCount)
    }
}

private fun job(
    id: Long = 0L,
    jobType: String,
    totalCount: Int,
): AudioGenerationJob =
    AudioGenerationJob(
        id = id,
        jobType = jobType,
        sourceType = "dictionary_cache",
        scopeType = AudioGenerationScope.ACTIVE_BOOK.scopeType,
        scopeRef = "cet4",
        status = AudioGenerationJobStatus.QUEUED.storageValue,
        totalCount = totalCount,
        createdAt = Instant.parse("2026-03-24T10:00:00Z"),
        updatedAt = Instant.parse("2026-03-24T10:00:00Z"),
    )

private class FakeAudioGenerationJobDao : AudioGenerationJobDao {
    private val jobs = mutableListOf<AudioGenerationJobEntity>()
    private var nextId = 1L

    override suspend fun insertJob(job: AudioGenerationJobEntity): Long {
        val resolvedId = job.id.takeIf { it > 0 } ?: nextId++
        jobs.removeAll { it.id == resolvedId }
        jobs += job.copy(id = resolvedId)
        return resolvedId
    }

    override suspend fun updateJob(job: AudioGenerationJobEntity) {
        jobs.removeAll { it.id == job.id }
        jobs += job
    }

    override suspend fun getJobById(jobId: Long): AudioGenerationJobEntity? =
        jobs.firstOrNull { it.id == jobId }

    override suspend fun getAllJobs(): List<AudioGenerationJobEntity> =
        jobs.sortedByDescending(AudioGenerationJobEntity::updatedAt)

    override suspend fun clearJobs() {
        jobs.clear()
    }
}
