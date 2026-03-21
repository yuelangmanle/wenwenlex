package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.ConfusionEdge
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.FeedbackBucket
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.PronunciationUsageSnapshot
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.WeeklySummary
import com.yueliangmanle.danci.core.model.metadataEntries
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

data class AggregatedAnalytics(
    val dailySummaries: List<DailySummary> = emptyList(),
    val weeklySummaries: List<WeeklySummary> = emptyList(),
    val learnerProfile: LearnerProfile = LearnerProfile(),
    val confusionEdges: List<ConfusionEdge> = emptyList(),
    val feedbackBreakdown: List<FeedbackBucket> = emptyList(),
    val pronunciationUsage: PronunciationUsageSnapshot = PronunciationUsageSnapshot(),
    val events: List<StudyEvent> = emptyList(),
)

class StudyAnalyticsAggregator(
    private val zoneId: ZoneId = ZoneOffset.UTC,
) {
    fun aggregate(
        events: List<StudyEvent>,
        referenceTime: Instant = Instant.now(),
    ): AggregatedAnalytics {
        val sortedEvents = events.sortedBy(StudyEvent::happenedAt)
        val dailySummaries = buildDailySummaries(sortedEvents, referenceTime)
        val weeklySummaries = buildWeeklySummaries(sortedEvents, referenceTime)
        val confusionEdges = buildConfusionEdges(sortedEvents, referenceTime)
        val learnerProfile = buildLearnerProfile(sortedEvents, confusionEdges, referenceTime)
        val feedbackBreakdown = buildFeedbackBreakdown(sortedEvents)
        val pronunciationUsage = buildPronunciationUsage(sortedEvents)

        return AggregatedAnalytics(
            dailySummaries = dailySummaries,
            weeklySummaries = weeklySummaries,
            learnerProfile = learnerProfile,
            confusionEdges = confusionEdges,
            feedbackBreakdown = feedbackBreakdown,
            pronunciationUsage = pronunciationUsage,
            events = sortedEvents,
        )
    }

    private fun buildDailySummaries(
        events: List<StudyEvent>,
        referenceTime: Instant,
    ): List<DailySummary> =
        events
            .groupBy { event -> event.happenedAt.atZone(zoneId).toLocalDate() }
            .toSortedMap()
            .map { (date, dayEvents) ->
                val answerEvents = dayEvents.performanceAnswerEvents()
                val wrongEvents = answerEvents.filter { it.isCorrect == false }
                val reviewCount = answerEvents.size
                val correctRate = if (reviewCount == 0) {
                    0f
                } else {
                    answerEvents.count { it.isCorrect == true }.toFloat() / reviewCount
                }
                val averageElapsed = answerEvents.mapNotNull(StudyEvent::elapsedMillis).average()
                val fatigueNote = when {
                    reviewCount >= 8 && averageElapsed >= 10_000 && correctRate < 0.7f -> "后段反应变慢"
                    wrongEvents.size >= 4 -> "错题密度偏高"
                    else -> null
                }

                DailySummary(
                    date = date.toString(),
                    studiedCount = dayEvents.map(StudyEvent::wordId).distinct().size,
                    reviewCount = reviewCount,
                    correctRate = correctRate,
                    fatigueNote = fatigueNote,
                    primaryMistakeReasons = topMistakeReasons(wrongEvents),
                    updatedAt = referenceTime,
                )
            }

    private fun buildWeeklySummaries(
        events: List<StudyEvent>,
        referenceTime: Instant,
    ): List<WeeklySummary> =
        events
            .groupBy { event ->
                event.happenedAt
                    .atZone(zoneId)
                    .toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }
            .toSortedMap()
            .map { (weekStartDate, weekEvents) ->
                val answerEvents = weekEvents.performanceAnswerEvents()
                val reviewCount = answerEvents.size
                val correctRate = if (reviewCount == 0) {
                    0f
                } else {
                    answerEvents.count { it.isCorrect == true }.toFloat() / reviewCount
                }

                WeeklySummary(
                    weekStartDate = weekStartDate.toString(),
                    studiedCount = weekEvents.map(StudyEvent::wordId).distinct().size,
                    correctRate = correctRate,
                    trendSummary = weeklyTrendSummary(correctRate),
                    persistentWeakSpots = answerEvents
                        .filter { it.isCorrect == false }
                        .map { event -> "word:${event.wordId}" }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key },
                    updatedAt = referenceTime,
                )
            }

    private fun buildConfusionEdges(
        events: List<StudyEvent>,
        referenceTime: Instant,
    ): List<ConfusionEdge> =
        events
            .filter { it.isPerformanceAnswerEvent() && it.isCorrect == false }
            .mapNotNull { event ->
                val metadata = event.metadataEntries()
                val targetWordId = metadata["confusedWordId"]?.toLongOrNull() ?: return@mapNotNull null
                Triple(
                    event.wordId,
                    targetWordId,
                    metadata["relationType"] ?: "confused_with",
                )
            }
            .groupBy { it }
            .map { (relation, groupedEvents) ->
                ConfusionEdge(
                    sourceWordId = relation.first,
                    targetWordId = relation.second,
                    relationType = relation.third,
                    weight = groupedEvents.size.toFloat(),
                    mistakeCount = groupedEvents.size,
                    updatedAt = referenceTime,
                )
            }
            .sortedWith(
                compareByDescending<ConfusionEdge> { it.mistakeCount }
                    .thenByDescending { it.weight },
            )

    private fun buildLearnerProfile(
        events: List<StudyEvent>,
        confusionEdges: List<ConfusionEdge>,
        referenceTime: Instant,
    ): LearnerProfile {
        val answerEvents = events.performanceAnswerEvents()
        val correctRate = if (answerEvents.isEmpty()) {
            0f
        } else {
            answerEvents.count { it.isCorrect == true }.toFloat() / answerEvents.size
        }

        val weakSpots = answerEvents
            .filter { it.isCorrect == false }
            .map { event -> "word:${event.wordId}" }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(5)
            .ifEmpty {
                confusionEdges.map { edge -> "word:${edge.sourceWordId}" }.distinct().take(5)
            }

        val preferredQuestionTypes = events
            .mapNotNull { event ->
                val metadata = event.metadataEntries()
                metadata["mode"] ?: event.eventType.substringBefore('_').takeIf(String::isNotBlank)
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        val commonMistakePatterns = answerEvents
            .filter { it.isCorrect == false }
            .map { event ->
                val metadata = event.metadataEntries()
                metadata["relationType"] ?: event.feedback ?: "记忆不稳"
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(4)

        return LearnerProfile(
            vocabularyLevel = vocabularyLevelFor(correctRate, answerEvents.isEmpty()),
            weakSpots = weakSpots,
            preferredQuestionTypes = preferredQuestionTypes,
            commonMistakePatterns = commonMistakePatterns,
            updatedAt = referenceTime,
        )
    }

    private fun buildFeedbackBreakdown(events: List<StudyEvent>): List<FeedbackBucket> {
        val feedbackCounts = events
            .mapNotNull { it.feedback?.trim()?.takeIf { feedback -> feedback.isNotEmpty() } }
            .groupingBy { it }
            .eachCount()

        val totalCount = feedbackCounts.values.sum()
        if (totalCount == 0) {
            return emptyList()
        }

        return feedbackCounts
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { it.key },
            )
            .map { (label, count) ->
                FeedbackBucket(
                    label = label,
                    count = count,
                    ratio = count.toFloat() / totalCount,
                )
            }
    }

    private fun buildPronunciationUsage(events: List<StudyEvent>): PronunciationUsageSnapshot {
        val audioEvents = events.filter { it.eventType == StudyEventType.AUDIO_PLAYED }
        if (audioEvents.isEmpty()) {
            return PronunciationUsageSnapshot()
        }

        val followReadContexts = setOf("study", "follow_read")
        return PronunciationUsageSnapshot(
            voicePlaybackCount = audioEvents.size,
            followReadCount = audioEvents.count { event ->
                event.metadataEntries()["play_context"] in followReadContexts
            },
            shadowingCount = audioEvents.count { event ->
                event.metadataEntries()["play_context"] == "shadowing"
            },
        )
    }

    private fun topMistakeReasons(events: List<StudyEvent>): List<String> =
        events
            .map { event ->
                val metadata = event.metadataEntries()
                metadata["relationType"] ?: event.feedback ?: "记忆不稳"
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

    private fun weeklyTrendSummary(correctRate: Float): String =
        when {
            correctRate >= 0.85f -> "本周掌握较稳"
            correctRate >= 0.7f -> "本周保持推进"
            correctRate > 0f -> "本周需要回拉基础"
            else -> "本周样本不足"
        }

    private fun vocabularyLevelFor(
        correctRate: Float,
        isEmpty: Boolean,
    ): String? =
        when {
            isEmpty -> null
            correctRate >= 0.85f -> "稳固推进"
            correctRate >= 0.7f -> "提升中"
            else -> "基础巩固"
        }
}
