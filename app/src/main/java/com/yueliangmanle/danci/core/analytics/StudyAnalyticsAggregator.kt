package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.ConfusionEdge
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.WeeklySummary
import com.yueliangmanle.danci.core.model.metadataEntries
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

data class AggregatedAnalytics(
    val dailySummaries: List<DailySummary> = emptyList(),
    val weeklySummaries: List<WeeklySummary> = emptyList(),
    val learnerProfile: LearnerProfile = LearnerProfile(),
    val confusionEdges: List<ConfusionEdge> = emptyList(),
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

        return AggregatedAnalytics(
            dailySummaries = dailySummaries,
            weeklySummaries = weeklySummaries,
            learnerProfile = learnerProfile,
            confusionEdges = confusionEdges,
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
                val answerEvents = dayEvents.filter { it.isCorrect != null }
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
                val answerEvents = weekEvents.filter { it.isCorrect != null }
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
                    persistentWeakSpots = weekEvents
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
            .filter { it.isCorrect == false }
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
        val answerEvents = events.filter { it.isCorrect != null }
        val correctRate = if (answerEvents.isEmpty()) {
            0f
        } else {
            answerEvents.count { it.isCorrect == true }.toFloat() / answerEvents.size
        }

        val weakSpots = events
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

        val commonMistakePatterns = events
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
