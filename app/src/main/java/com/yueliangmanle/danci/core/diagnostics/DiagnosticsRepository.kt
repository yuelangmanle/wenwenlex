package com.yueliangmanle.danci.core.diagnostics

import android.content.Context
import com.yueliangmanle.danci.BuildConfig
import com.yueliangmanle.danci.core.data.AiProfileRepository
import com.yueliangmanle.danci.core.data.BackupRepository
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.buildAiProfileRepository
import com.yueliangmanle.danci.core.data.buildBackupRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.database.DanciDatabase
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.GoalProgressSnapshot
import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.io.File
import java.time.Instant

class DiagnosticsRepository(
    private val database: DanciDatabase,
    private val settingsRepository: SettingsRepository,
    private val studyRepository: StudyRepository,
    private val aiProfileRepository: AiProfileRepository,
    private val backupRepository: BackupRepository,
    private val voicePackRepository: VoicePackRepository,
    private val verifier: DataIntegrityVerifier = DataIntegrityVerifier(),
    private val exporterFactory: (File) -> DiagnosticsExporter = { DiagnosticsExporter(it) },
    private val nowProvider: () -> Instant = { Instant.now() },
    private val appVersionNameProvider: () -> String = { BuildConfig.VERSION_NAME },
    private val appVersionCodeProvider: () -> Int = { BuildConfig.VERSION_CODE },
    private val exportDirProvider: () -> File,
) {
    suspend fun buildReport(): DiagnosticsReport {
        val checkedAt = nowProvider()
        val settings = settingsRepository.getSettings()
        val records = studyRepository.getAllLearningRecords()
        val events = studyRepository.getAllStudyEvents()
        val books = database.bookDao().getAllBooks()
        val aiProfiles = aiProfileRepository.getProfiles()
        val voicePacks = voicePackRepository.getAllVoicePacks()
        val latestBackup = backupRepository.latestBackupFile()
        val aiMemorySummary = studyRepository.loadAiMemorySummary(planLimit = 50)
        val snapshot = DataIntegritySnapshot(
            checkedAt = checkedAt,
            settings = settings,
            learningRecordCount = records.size,
            studyEventCount = events.size,
            bookCount = books.size,
            aiProfileCount = aiProfiles.size,
            installedVoicePackCount = voicePacks.count { pack ->
                pack.status == VoicePackStatus.READY.storageValue
            },
            hasBackup = latestBackup != null,
            latestBackupName = latestBackup?.name,
            latestBackupAt = latestBackup?.let { Instant.ofEpochMilli(it.lastModified()) },
            upgradeHealth = aiMemorySummary.upgradeHealth,
        )
        val healthReport = verifier.verify(snapshot)
        val summaryLines = buildSummaryLines(
            appVersionName = appVersionNameProvider(),
            appVersionCode = appVersionCodeProvider(),
            snapshot = snapshot,
            healthReport = healthReport,
            goalProgress = aiMemorySummary.goalProgress,
        )
        return DiagnosticsReport(
            generatedAt = checkedAt,
            appVersionName = appVersionNameProvider(),
            appVersionCode = appVersionCodeProvider(),
            issueCount = healthReport.issues.size,
            issues = healthReport.issues,
            upgradeHealth = healthReport.toUpgradeHealthMap(
                appVersionName = appVersionNameProvider(),
                appVersionCode = appVersionCodeProvider(),
            ),
            summaryLines = summaryLines,
            snapshot = snapshot,
        )
    }

    suspend fun exportReport(report: DiagnosticsReport): File =
        exporterFactory(exportDirProvider()).export(report)

    private fun buildSummaryLines(
        appVersionName: String,
        appVersionCode: Int,
        snapshot: DataIntegritySnapshot,
        healthReport: UpgradeHealthReport,
        goalProgress: GoalProgressSnapshot,
    ): List<String> =
        buildList {
            add("版本：$appVersionName ($appVersionCode)")
            add("检查时间：${snapshot.checkedAt}")
            add("状态：${healthReport.status}")
            add("问题数：${healthReport.issues.size}")
            add("学习记录：${snapshot.learningRecordCount}")
            add("学习事件：${snapshot.studyEventCount}")
            add("词书数量：${snapshot.bookCount}")
            add("AI 档案：${snapshot.aiProfileCount}")
            add("已装语音包：${snapshot.installedVoicePackCount}")
            add(
                if (snapshot.hasBackup) {
                    "最近备份：${snapshot.latestBackupName ?: "已存在"}"
                } else {
                    "最近备份：无"
                },
            )
            add(
                "目标推进：今日 ${goalProgress.currentDayCompletedCount} / 本周 ${goalProgress.currentWeekCompletedCount}",
            )
        }
}

private fun UpgradeHealthReport.toUpgradeHealthMap(
    appVersionName: String,
    appVersionCode: Int,
): Map<String, String> =
    linkedMapOf(
        "status" to status,
        "checked_at" to checkedAt.toString(),
        "issue_count" to issues.size.toString(),
        "summary" to summary,
        "issue_codes" to issues.joinToString(",") { it.code },
        "app_version_name" to appVersionName,
        "app_version_code" to appVersionCode.toString(),
    )

fun buildDiagnosticsRepository(context: Context): DiagnosticsRepository {
    val appContext = context.applicationContext
    val database = buildDanciDatabase(appContext)
    return DiagnosticsRepository(
        database = database,
        settingsRepository = buildSettingsRepository(appContext),
        studyRepository = RoomStudyRepository(database.studyDao()),
        aiProfileRepository = buildAiProfileRepository(appContext),
        backupRepository = buildBackupRepository(appContext),
        voicePackRepository = buildVoicePackRepository(appContext),
        exportDirProvider = {
            appContext.getExternalFilesDir("diagnostics") ?: File(appContext.filesDir, "diagnostics")
        },
    )
}
