package com.yueliangmanle.danci.feature.home

import android.content.Context
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.parseBuiltInCatalog
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.study.ReviewScheduler
import com.yueliangmanle.danci.core.study.TodayTaskEngine
import java.time.Instant
import kotlin.math.max

data class HomeUiState(
    val isLoading: Boolean = false,
    val headline: String = "",
    val todayGoalCount: Int = 0,
    val completedCount: Int = 0,
    val newWordCount: Int = 0,
    val reviewCount: Int = 0,
    val mistakeCount: Int = 0,
    val estimatedMinutes: Int = 0,
    val streakDays: Int = 0,
    val activeBookTitle: String = "",
    val aiSuggestion: String? = null,
) {
    companion object {
        fun loading(): HomeUiState = HomeUiState(isLoading = true)
    }
}

class HomeViewModel(
    private val settings: AppSettings,
    private val builtInBooks: List<BuiltInBookCatalogItem>,
    private val learningRecords: List<LearningRecord> = emptyList(),
    private val todayTaskEngine: TodayTaskEngine = TodayTaskEngine(),
    private val reviewScheduler: ReviewScheduler = ReviewScheduler(),
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    fun buildUiState(): HomeUiState {
        val activeBook = selectActiveBook()
        val reviewSummary = reviewScheduler.summarize(
            learningRecords = learningRecords,
            now = nowProvider(),
        )
        val dailyGoal = settings.dailyGoal
        val unseenWords = max(dailyGoal, activeBook?.wordCount ?: dailyGoal)
        val plan = todayTaskEngine.build(
            dailyGoal = dailyGoal,
            overdueWords = reviewSummary.overdueWords,
            unseenWords = unseenWords,
            recentMistakeWords = reviewSummary.recentMistakeWords,
        )
        val plannedStudyCount = plan.newWordCount + plan.reviewCount

        return HomeUiState(
            headline = "今天还要学 $plannedStudyCount 个词",
            todayGoalCount = dailyGoal,
            completedCount = 0,
            newWordCount = plan.newWordCount,
            reviewCount = plan.reviewCount,
            mistakeCount = plan.mistakeCount,
            estimatedMinutes = plan.estimatedMinutes,
            streakDays = reviewSummary.streakDays,
            activeBookTitle = activeBook?.title ?: "还未选择词书",
            aiSuggestion = if (plan.mistakeCount > 0) {
                "先处理错词，再开始今天的新词，能更稳地拉回记忆。"
            } else {
                "今天以稳住节奏为主，先完成首页任务。"
            },
        )
    }

    private fun selectActiveBook(): BuiltInBookCatalogItem? =
        builtInBooks.firstOrNull { it.id == settings.activeBookId } ?: builtInBooks.firstOrNull()
}

suspend fun loadHomeViewModel(context: Context): HomeViewModel {
    val settings = buildSettingsRepository(context).getSettings()
    val builtInBooks = context.assets.open("books/manifest.json").use(::parseBuiltInCatalog)
    return HomeViewModel(
        settings = settings,
        builtInBooks = builtInBooks,
    )
}
