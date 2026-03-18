package com.yueliangmanle.danci.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import java.util.concurrent.TimeUnit

const val AI_SUMMARY_REFRESH_WORK_NAME = "ai_summary_refresh"

class AiSummaryRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result =
        runCatching {
            buildAiMemoryRepository(applicationContext).refreshMemorySummary()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}

class AiSummaryRefreshScheduler(
    context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<AiSummaryRefreshWorker>(12, TimeUnit.HOURS)
            .addTag(AI_SUMMARY_REFRESH_WORK_NAME)
            .build()
        workManager.enqueueUniquePeriodicWork(
            AI_SUMMARY_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
