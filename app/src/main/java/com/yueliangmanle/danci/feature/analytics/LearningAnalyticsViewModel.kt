package com.yueliangmanle.danci.feature.analytics

import android.content.Context
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.StudyRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.goal.GoalProgressTracker
import com.yueliangmanle.danci.core.model.GoalProgressSnapshot
import com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot
import com.yueliangmanle.danci.core.data.AppSettings
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AnalyticsCardUiModel(
    val label: String,
    val value: String,
    val supportingText: String? = null,
)

data class LearningAnalyticsUiState(
    val isLoading: Boolean = false,
    val title: String = "学习统计",
    val subtitle: String = "把最近的节奏、反馈和计划效果放在一起看。",
    val overviewCards: List<AnalyticsCardUiModel> = emptyList(),
    val goalProgressCards: List<AnalyticsCardUiModel> = emptyList(),
    val chartHtml: String? = null,
    val insightBullets: List<String> = emptyList(),
    val errorMessage: String? = null,
)

class LearningAnalyticsViewModel(
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val goalProgressTracker: GoalProgressTracker = GoalProgressTracker(),
    private val htmlRenderer: LearningAnalyticsHtmlRenderer = LearningAnalyticsHtmlRenderer(),
) {
    suspend fun loadUiState(): LearningAnalyticsUiState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val summary = studyRepository.loadAiMemorySummary(planLimit = 50)
        val goalProgress = goalProgressTracker.build(
            records = studyRepository.getAllLearningRecords(),
            settings = settings,
            now = java.time.Instant.now(),
            studyEvents = studyRepository.getAllStudyEvents(),
        )
        val snapshot = summary.analyticsSnapshot

        LearningAnalyticsUiState(
            overviewCards = snapshot.toOverviewCards(),
            goalProgressCards = goalProgress.toGoalProgressCards(settings),
            chartHtml = htmlRenderer.render(snapshot, settings, goalProgress),
            insightBullets = summary.longTermInsights.ifEmpty {
                listOf("近期统计样本仍少，先继续完成学习任务，等数据更稳定后再看趋势。")
            },
        )
    }
}

suspend fun loadLearningAnalyticsViewModel(context: Context): LearningAnalyticsViewModel =
    withContext(Dispatchers.IO) {
        LearningAnalyticsViewModel(
            studyRepository = RoomStudyRepository(buildDanciDatabase(context.applicationContext).studyDao()),
            settingsRepository = buildSettingsRepository(context),
        )
    }

private fun LearningAnalyticsSnapshot.toOverviewCards(): List<AnalyticsCardUiModel> {
    val pronunciationUsageCount = pronunciationUsage.followReadCount +
        pronunciationUsage.voicePlaybackCount +
        pronunciationUsage.shadowingCount

    return listOf(
        AnalyticsCardUiModel(
            label = "正确率",
            value = overview.accuracyRate?.let(::formatPercent) ?: "暂无",
            supportingText = "最近答题表现",
        ),
        AnalyticsCardUiModel(
            label = "学习天数",
            value = overview.studiedDays.toString(),
            supportingText = "最近统计窗口",
        ),
        AnalyticsCardUiModel(
            label = "已掌握",
            value = overview.masteredCount.toString(),
            supportingText = "去重单词数",
        ),
        AnalyticsCardUiModel(
            label = "发音使用",
            value = pronunciationUsageCount.toString(),
            supportingText = "播音 / 跟读 / shadowing",
        ),
    )
}

private fun GoalProgressSnapshot.toGoalProgressCards(settings: AppSettings): List<AnalyticsCardUiModel> =
    buildList {
        add(
            AnalyticsCardUiModel(
                label = "本周目标",
                value = "${currentWeekCompletedCount} / ${settings.weeklyGoal}",
                supportingText = "今日完成 ${currentDayCompletedCount} 词",
            ),
        )
        add(
            AnalyticsCardUiModel(
                label = "连续学习",
                value = "${currentStreakDays} 天",
                supportingText = "最佳记录 ${bestStreakDays} 天",
            ),
        )
        if (!phaseName.isNullOrBlank() || phaseTargetWords > 0) {
            add(
                AnalyticsCardUiModel(
                    label = "当前阶段",
                    value = if (phaseTargetWords > 0) {
                        "${phaseName ?: "当前阶段"} · $phaseCompletedWords / $phaseTargetWords"
                    } else {
                        "${phaseName ?: "当前阶段"} · 已完成 $phaseCompletedWords 词"
                    },
                    supportingText = "阶段推进",
                ),
            )
        }
    }

private fun formatPercent(value: Float): String = "${(value * 100).roundToInt()}%"
