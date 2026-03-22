package com.yueliangmanle.danci.core.goal

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.model.GoalProgressSnapshot
import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventMetadataKey
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.metadataEntries
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class GoalProgressTracker(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun build(
        records: List<LearningRecord>,
        settings: AppSettings,
        now: Instant,
        studyEvents: List<StudyEvent> = emptyList(),
    ): GoalProgressSnapshot {
        val today = now.atZone(zoneId).toLocalDate()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val activityByDate = buildActivityByDate(records = records, studyEvents = studyEvents)
        val currentDayCompletedCount = activityByDate[today]?.size ?: 0
        val currentWeekCompletedCount = activityByDate
            .filterKeys { date -> !date.isBefore(weekStart) && !date.isAfter(today) }
            .values
            .flatten()
            .toSet()
            .size
        val activityDates = activityByDate.keys

        return GoalProgressSnapshot(
            currentDayCompletedCount = currentDayCompletedCount,
            currentWeekCompletedCount = currentWeekCompletedCount,
            currentStreakDays = calculateCurrentStreak(activityDates, today),
            bestStreakDays = calculateBestStreak(activityDates),
            phaseName = settings.phaseName,
            phaseTargetWords = settings.phaseTargetWords,
            phaseCompletedWords = records.count { record ->
                record.reviewCount > 0 || record.lastReviewedAt != null
            },
        )
    }

    private fun buildActivityByDate(
        records: List<LearningRecord>,
        studyEvents: List<StudyEvent>,
    ): Map<LocalDate, Set<Long>> {
        val eventBased = studyEvents
            .asSequence()
            .filter { event -> event.eventType == StudyEventType.CARD_FEEDBACK }
            .filterNot { event ->
                event.metadataEntries()[StudyEventMetadataKey.SKIPPED] == "true"
            }
            .groupBy(
                keySelector = { event -> event.happenedAt.atZone(zoneId).toLocalDate() },
                valueTransform = StudyEvent::wordId,
            )
            .mapValues { (_, wordIds) -> wordIds.toSet() }

        if (eventBased.isNotEmpty()) {
            return eventBased
        }

        return records
            .asSequence()
            .mapNotNull { record ->
                val reviewedAt = record.lastReviewedAt ?: return@mapNotNull null
                reviewedAt.atZone(zoneId).toLocalDate() to record.wordId
            }
            .groupBy(
                keySelector = Pair<LocalDate, Long>::first,
                valueTransform = Pair<LocalDate, Long>::second,
            )
            .mapValues { (_, wordIds) -> wordIds.toSet() }
    }

    private fun calculateCurrentStreak(
        activityDates: Set<LocalDate>,
        today: LocalDate,
    ): Int {
        if (activityDates.isEmpty()) {
            return 0
        }
        var streak = 0
        var cursor = today
        while (activityDates.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun calculateBestStreak(activityDates: Set<LocalDate>): Int {
        if (activityDates.isEmpty()) {
            return 0
        }
        val orderedDates = activityDates.toList().sorted()
        var best = 1
        var running = 1
        orderedDates.zipWithNext().forEach { (previous, current) ->
            if (current == previous.plusDays(1)) {
                running += 1
                best = maxOf(best, running)
            } else {
                running = 1
            }
        }
        return best
    }
}
