package com.yueliangmanle.danci.feature.home

import android.content.Context
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.ai.AiPlanAdjustmentResult
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.syncBuiltInCatalogToDatabase
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.study.ReviewScheduler
import com.yueliangmanle.danci.core.study.TodayTaskEngine
import java.time.Instant
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = false,
    val isAnalyzingPlan: Boolean = false,
    val errorMessage: String? = null,
    val headline: String = "",
    val todayGoalCount: Int = 0,
    val completedCount: Int = 0,
    val newWordCount: Int = 0,
    val reviewCount: Int = 0,
    val mistakeCount: Int = 0,
    val estimatedMinutes: Int = 0,
    val streakDays: Int = 0,
    val activeBookTitle: String = "",
    val aiSuggestionTitle: String? = null,
    val aiSuggestion: String? = null,
    val aiSuggestionMeta: String? = null,
    val aiFocusWords: List<String> = emptyList(),
) {
    companion object {
        fun loading(): HomeUiState = HomeUiState(isLoading = true)
    }
}

class HomeViewModel(
    private val settings: AppSettings,
    private val books: List<Book>,
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
            aiSuggestionTitle = "今日节奏建议",
            aiSuggestion = if (plan.mistakeCount > 0) {
                "先处理错词，再开始今天的新词，能更稳地拉回记忆。"
            } else {
                "今天以稳住节奏为主，先完成首页任务。"
            },
        )
    }

    fun markAnalyzing(current: HomeUiState): HomeUiState =
        current.copy(
            isAnalyzingPlan = true,
            errorMessage = null,
            aiSuggestionTitle = "AI 正在分析",
            aiSuggestion = "正在整理最近的学习表现和薄弱点，请稍候。",
            aiSuggestionMeta = null,
        )

    fun applyPlanAdjustment(
        current: HomeUiState,
        result: AiPlanAdjustmentResult,
    ): HomeUiState =
        current.copy(
            isAnalyzingPlan = false,
            errorMessage = null,
            aiSuggestionTitle = if (result.source == PlanSource.AI) {
                "AI 计划调整"
            } else {
                "本地兜底建议"
            },
            aiSuggestion = listOfNotNull(result.summary, result.checkpointAdvice).joinToString("\n"),
            aiSuggestionMeta = listOfNotNull(
                if (result.source == PlanSource.AI) "来源：AI" else "来源：本地规则",
                result.suggestedPace?.let { "节奏：$it" },
            ).joinToString(" · ").takeIf(String::isNotBlank),
            aiFocusWords = result.recommendedFocus,
        )

    private fun selectActiveBook(): Book? =
        books.firstOrNull { it.id == settings.activeBookId } ?: books.firstOrNull()
}

suspend fun loadHomeViewModel(context: Context): HomeViewModel {
    return withContext(Dispatchers.IO) {
        syncBuiltInCatalogToDatabase(context)
        val settings = buildSettingsRepository(context).getSettings()
        val books = buildBookRepository(context).getAllBooks()
        HomeViewModel(
            settings = settings,
            books = books,
        )
    }
}
