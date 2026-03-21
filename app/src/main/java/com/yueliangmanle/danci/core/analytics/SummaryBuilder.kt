package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.ConfusionEdge
import com.yueliangmanle.danci.core.model.CheckpointSummary
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.FeedbackBucket
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.LearningAnalyticsSnapshot
import com.yueliangmanle.danci.core.model.PlanEffectSnapshot
import com.yueliangmanle.danci.core.model.PronunciationUsageSnapshot
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.WeeklySummary
import com.yueliangmanle.danci.core.model.metadataEntries
import java.time.Instant
import kotlin.math.roundToInt

data class StudyEventSample(
    val wordId: Long,
    val eventType: String,
    val feedback: String? = null,
    val isCorrect: Boolean? = null,
    val happenedAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
)

data class SummaryContext(
    val dailyTrend: List<DailySummary> = emptyList(),
    val weeklyTrend: List<WeeklySummary> = emptyList(),
    val rawSamples: List<StudyEventSample> = emptyList(),
    val learnerProfile: LearnerProfile? = null,
    val confusionHighlights: List<ConfusionEdge> = emptyList(),
    val analyticsSnapshot: LearningAnalyticsSnapshot? = null,
    val longTermInsights: List<String> = emptyList(),
    val planEffects: List<PlanEffectSnapshot> = emptyList(),
)

class SummaryBuilder {
    fun buildContext(
        sevenDay: List<DailySummary>,
        thirtyDay: List<WeeklySummary>,
        rawEvents: List<StudyEvent>,
        learnerProfile: LearnerProfile? = null,
        confusionEdges: List<ConfusionEdge> = emptyList(),
        analyticsSnapshot: LearningAnalyticsSnapshot? = null,
        longTermInsights: List<String> = emptyList(),
        planEffects: List<PlanEffectSnapshot> = emptyList(),
        checkpointSummaries: List<CheckpointSummary> = emptyList(),
    ): SummaryContext {
        val resolvedPlanEffects = planEffects.ifEmpty { analyticsSnapshot?.planEffects.orEmpty() }

        return SummaryContext(
            dailyTrend = sevenDay.sortedBy(DailySummary::date).takeLast(7),
            weeklyTrend = thirtyDay.sortedBy(WeeklySummary::weekStartDate).takeLast(4),
            rawSamples = prioritizedSamples(rawEvents).map(::asStudyEventSample),
            learnerProfile = learnerProfile,
            confusionHighlights = confusionEdges
                .sortedWith(
                    compareByDescending<ConfusionEdge> { it.mistakeCount }
                        .thenByDescending { it.weight },
                )
                .take(10),
            analyticsSnapshot = analyticsSnapshot,
            longTermInsights = longTermInsights.ifEmpty {
                buildLongTermInsights(
                    analyticsSnapshot = analyticsSnapshot,
                    learnerProfile = learnerProfile,
                    weeklyTrend = thirtyDay,
                    checkpointSummaries = checkpointSummaries,
                    planEffects = resolvedPlanEffects,
                )
            },
            planEffects = resolvedPlanEffects,
        )
    }

    fun buildLongTermInsights(
        analyticsSnapshot: LearningAnalyticsSnapshot? = null,
        learnerProfile: LearnerProfile? = null,
        weeklyTrend: List<WeeklySummary> = emptyList(),
        checkpointSummaries: List<CheckpointSummary> = emptyList(),
        planEffects: List<PlanEffectSnapshot> = emptyList(),
    ): List<String> {
        val resolvedPlanEffects = planEffects.ifEmpty { analyticsSnapshot?.planEffects.orEmpty() }
        val insights = buildList {
            buildRecentAdjustmentInsight(resolvedPlanEffects, checkpointSummaries)?.let(::add)
            buildWeakSpotInsight(learnerProfile, weeklyTrend)?.let(::add)
            buildPronunciationInsight(analyticsSnapshot?.pronunciationUsage)?.let(::add)
            buildFeedbackInsight(analyticsSnapshot?.feedbackBreakdown.orEmpty())?.let(::add)
        }.distinct()

        return insights.take(4).ifEmpty { listOf("近期统计样本仍少，先保持当前节奏继续观察。") }
    }

    private fun prioritizedSamples(rawEvents: List<StudyEvent>): List<StudyEvent> =
        rawEvents
            .sortedWith(
                compareByDescending<StudyEvent> { importanceScore(it) }
                    .thenByDescending { it.happenedAt },
            )
            .take(20)

    private fun importanceScore(event: StudyEvent): Int =
        when {
            event.isCorrect == false -> 3
            event.eventType == StudyEventType.AI_ACTION -> 2
            event.eventType == StudyEventType.DETAIL_OPENED -> 2
            event.eventType == StudyEventType.QUIZ_STARTED -> 2
            else -> 1
        }

    private fun asStudyEventSample(event: StudyEvent): StudyEventSample =
        StudyEventSample(
            wordId = event.wordId,
            eventType = event.eventType,
            feedback = event.feedback,
            isCorrect = event.isCorrect,
            happenedAt = event.happenedAt,
            metadata = event.metadataEntries(),
        )

    private fun buildRecentAdjustmentInsight(
        planEffects: List<PlanEffectSnapshot>,
        checkpointSummaries: List<CheckpointSummary>,
    ): String? {
        val latestEffect = planEffects.firstOrNull()
        if (latestEffect != null) {
            return buildString {
                append("最近一次调整")
                if (latestEffect.label.isNotBlank()) {
                    append("「${latestEffect.label}」")
                }
                append("后")
                append(latestEffect.outcomeSummary ?: "效果仍在观察")
                latestEffect.afterCorrectRate?.let { append("，当前窗口正确率约 ${formatPercent(it)}") }
                append("。")
            }
        }

        val latestCheckpoint = checkpointSummaries.maxByOrNull(CheckpointSummary::windowEndAt) ?: return null
        return "最近检查点显示${latestCheckpoint.effectSummary}，当前重点信号是${latestCheckpoint.signalSummary}。"
    }

    private fun buildWeakSpotInsight(
        learnerProfile: LearnerProfile?,
        weeklyTrend: List<WeeklySummary>,
    ): String? {
        val weakSpots = learnerProfile?.weakSpots?.take(2).orEmpty()
        val weeklyWeakSpots = weeklyTrend
            .lastOrNull()
            ?.persistentWeakSpots
            ?.take(2)
            .orEmpty()
        val preferredTypes = learnerProfile?.preferredQuestionTypes?.take(2).orEmpty()

        val parts = buildList {
            val mergedWeakSpots = (weakSpots + weeklyWeakSpots).distinct().take(3)
            if (mergedWeakSpots.isNotEmpty()) {
                add("长期薄弱点集中在 ${mergedWeakSpots.joinToString("、")}")
            }
            if (preferredTypes.isNotEmpty()) {
                add("近期题型倾向偏向 ${preferredTypes.joinToString("、")}")
            }
        }

        val summary = parts.joinToString("，")
        return summary.takeIf { it.isNotBlank() }?.plus("。")
    }

    private fun buildPronunciationInsight(usage: PronunciationUsageSnapshot?): String? {
        val snapshot = usage ?: return null
        val totalUsage = snapshot.followReadCount + snapshot.voicePlaybackCount + snapshot.shadowingCount
        if (totalUsage == 0) {
            return "近期几乎没有使用发音功能，可考虑把易混词加入跟读或听音复习。"
        }
        return "发音使用概况：跟读 ${snapshot.followReadCount} 次，播音 ${snapshot.voicePlaybackCount} 次，shadowing ${snapshot.shadowingCount} 次。"
    }

    private fun buildFeedbackInsight(feedbackBreakdown: List<FeedbackBucket>): String? {
        val topBucket = feedbackBreakdown.maxByOrNull(FeedbackBucket::count) ?: return null
        if (topBucket.count == 0) {
            return null
        }
        return "最近反馈以 ${topBucket.label} 为主，占比 ${formatPercent(topBucket.ratio)}。"
    }

    private fun formatPercent(value: Float): String = "${(value * 100).roundToInt()}%"
}
