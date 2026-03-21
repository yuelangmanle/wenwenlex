package com.yueliangmanle.danci.core.model

data class LearningAnalyticsSnapshot(
    val overview: AnalyticsOverview = AnalyticsOverview(),
    val dailyTrend: List<DailyTrendPoint> = emptyList(),
    val feedbackBreakdown: List<FeedbackBucket> = emptyList(),
    val bookProgress: List<BookProgressSnapshot> = emptyList(),
    val planEffects: List<PlanEffectSnapshot> = emptyList(),
    val pronunciationUsage: PronunciationUsageSnapshot = PronunciationUsageSnapshot(),
)

data class AnalyticsOverview(
    val accuracyRate: Float? = null,
    val studiedDays: Int = 0,
    val masteredCount: Int = 0,
)

data class DailyTrendPoint(
    val date: String = "",
    val studiedCount: Int = 0,
    val correctRate: Float = 0f,
)

data class FeedbackBucket(
    val label: String = "",
    val count: Int = 0,
    val ratio: Float = 0f,
)

data class BookProgressSnapshot(
    val bookId: String = "",
    val bookName: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
)

data class PlanEffectSnapshot(
    val planVersionId: Long = 0,
    val label: String = "",
    val beforeCorrectRate: Float? = null,
    val afterCorrectRate: Float? = null,
    val outcomeSummary: String? = null,
)

data class PronunciationUsageSnapshot(
    val followReadCount: Int = 0,
    val voicePlaybackCount: Int = 0,
    val shadowingCount: Int = 0,
)
