package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.AnalyticsOverview
import com.yueliangmanle.danci.core.model.BookProgressSnapshot
import com.yueliangmanle.danci.core.model.DailyTrendPoint
import com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot
import com.yueliangmanle.danci.core.model.PlanApplyStatus
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.StudyEvent

class LearningDashboardComposer(
    private val planEffectEvaluator: PlanEffectEvaluator = PlanEffectEvaluator(),
    private val maxPlanEffects: Int = 3,
) {
    fun compose(
        aggregated: AggregatedAnalytics,
        planHistory: List<PlanHistoryEntry>,
        activeBookTitle: String?,
    ): LearningAnalyticsSnapshot =
        LearningAnalyticsSnapshot(
            overview = buildOverview(aggregated),
            dailyTrend = aggregated.dailySummaries.map { summary ->
                DailyTrendPoint(
                    date = summary.date,
                    studiedCount = summary.studiedCount,
                    correctRate = summary.correctRate,
                )
            },
            feedbackBreakdown = aggregated.feedbackBreakdown,
            bookProgress = buildBookProgress(
                activeBookTitle = activeBookTitle,
                completedCount = buildMasteredCount(aggregated.events),
            ),
            planEffects = planHistory
                .filter { it.applyStatus == PlanApplyStatus.APPLIED }
                .sortedByDescending(PlanHistoryEntry::generatedAt)
                .take(maxPlanEffects)
                .map { planEffectEvaluator.evaluate(it, aggregated.events) },
            pronunciationUsage = aggregated.pronunciationUsage,
        )

    private fun buildOverview(aggregated: AggregatedAnalytics): AnalyticsOverview {
        val answerEvents = aggregated.events.filter { it.isCorrect != null }
        val accuracyRate = aggregated.weeklySummaries.lastOrNull()?.correctRate
            ?: answerEvents.takeIf { it.isNotEmpty() }?.let { events ->
                events.count { it.isCorrect == true }.toFloat() / events.size
            }

        return AnalyticsOverview(
            accuracyRate = accuracyRate,
            studiedDays = aggregated.dailySummaries.size,
            masteredCount = buildMasteredCount(answerEvents),
        )
    }

    private fun buildMasteredCount(events: List<StudyEvent>): Int =
        events
            .filter { it.isCorrect == true }
            .map(StudyEvent::wordId)
            .distinct()
            .size

    private fun buildBookProgress(
        activeBookTitle: String?,
        completedCount: Int,
    ): List<BookProgressSnapshot> =
        activeBookTitle
            ?.takeIf { title -> title.isNotBlank() }
            ?.let { title ->
                listOf(
                    BookProgressSnapshot(
                        bookId = "active_book",
                        bookName = title,
                        completedCount = completedCount,
                        totalCount = 0,
                    ),
                )
            }
            .orEmpty()
}
