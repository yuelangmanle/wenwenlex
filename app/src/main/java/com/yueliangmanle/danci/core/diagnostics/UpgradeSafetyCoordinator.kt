package com.yueliangmanle.danci.core.diagnostics

import android.content.Context
import com.yueliangmanle.danci.core.data.AiMemoryRepository
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.database.buildDanciDatabase

class UpgradeSafetyCoordinator(
    private val diagnosticsRepository: DiagnosticsRepository,
    private val studyRepository: StudyRepository,
    private val aiMemoryRepository: AiMemoryRepository,
) {
    suspend fun runPostUpgradeChecks(): UpgradeHealthReport {
        val report = diagnosticsRepository.buildReport()
        val currentSummary = studyRepository.loadAiMemorySummary(planLimit = 50)
        val updatedSummary = currentSummary.copy(upgradeHealth = report.upgradeHealth)
        studyRepository.saveAiMemorySummary(updatedSummary)
        aiMemoryRepository.refreshMemorySummary(referenceTime = report.generatedAt)
        return UpgradeHealthReport(
            checkedAt = report.generatedAt,
            status = report.upgradeHealth["status"].orEmpty(),
            summary = report.upgradeHealth["summary"].orEmpty(),
            issues = report.issues,
        )
    }
}

fun buildUpgradeSafetyCoordinator(context: Context): UpgradeSafetyCoordinator {
    val appContext = context.applicationContext
    val database = buildDanciDatabase(appContext)
    return UpgradeSafetyCoordinator(
        diagnosticsRepository = buildDiagnosticsRepository(appContext),
        studyRepository = RoomStudyRepository(database.studyDao()),
        aiMemoryRepository = buildAiMemoryRepository(appContext),
    )
}
