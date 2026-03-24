package com.yueliangmanle.danci.feature.home

import android.content.Context
import com.yueliangmanle.danci.core.ai.AiPlanAdjustmentResult
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.RoomBookRepository
import com.yueliangmanle.danci.core.data.RoomStudyRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.syncBuiltInCatalogToDatabase
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.study.ReviewScheduler
import com.yueliangmanle.danci.core.study.TodayTaskEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun buildUiState(): HomeUiState {
        val activeBook = selectActiveBook()
        val now = nowProvider()
        val today = LocalDate.ofInstant(now, zoneId)
        val reviewSummary = reviewScheduler.summarize(
            learningRecords = learningRecords,
            now = now,
            zoneId = zoneId,
        )
        val dailyGoal = settings.dailyGoal
        val introducedToday = learningRecords.count { record ->
            record.introducedAt?.let { isToday(it, today) } == true
        }
        val remainingNewWords = (dailyGoal - introducedToday).coerceAtLeast(0)
        val plan = todayTaskEngine.build(
            remainingNewWords = remainingNewWords,
            overdueWords = reviewSummary.overdueWords,
            recentMistakeWords = reviewSummary.recentMistakeWords,
        )
        val plannedStudyCount = plan.newWordCount + plan.reviewCount + plan.mistakeCount

        return HomeUiState(
            headline = "今天还要学 $plannedStudyCount 个词",
            todayGoalCount = dailyGoal,
            completedCount = introducedToday.coerceIn(0, dailyGoal),
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

    private fun isToday(
        instant: Instant,
        today: LocalDate,
    ): Boolean = LocalDate.ofInstant(instant, zoneId) == today
}

suspend fun loadHomeViewModel(context: Context): HomeViewModel {
    return withContext(Dispatchers.IO) {
        syncBuiltInCatalogToDatabase(context)
        val database = buildDanciDatabase(context.applicationContext)
        val settings = buildSettingsRepository(context).getSettings()
        val books = RoomBookRepository(database.bookDao()).getAllBooks()
        val learningRecords = RoomStudyRepository(database.studyDao()).getAllLearningRecords()
        HomeViewModel(
            settings = settings,
            books = books,
            learningRecords = learningRecords,
        )
    }
}
