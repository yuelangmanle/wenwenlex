package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.ConfusionEdge
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.WeeklySummary
import com.yueliangmanle.danci.core.model.metadataEntries
import java.time.Instant

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
)

class SummaryBuilder {
    fun buildContext(
        sevenDay: List<DailySummary>,
        thirtyDay: List<WeeklySummary>,
        rawEvents: List<StudyEvent>,
        learnerProfile: LearnerProfile? = null,
        confusionEdges: List<ConfusionEdge> = emptyList(),
    ): SummaryContext =
        SummaryContext(
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
        )

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
}
