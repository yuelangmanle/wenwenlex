package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.Word
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class StudyQueueEmptyState {
    NO_NEW_WORDS,
    NO_DUE_REVIEW,
    NO_RECENT_MISTAKES,
}

data class StudyQueuePlan(
    val queue: List<StudyCardItem>,
    val emptyState: StudyQueueEmptyState? = null,
)

class StudyQueuePlanner(
    private val queueBuilder: StudyQueueBuilder = StudyQueueBuilder(),
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    fun resolveMode(
        requestedMode: StudyLaunchMode?,
        words: List<Word>,
        recordsByWordId: Map<Long, LearningRecord>,
    ): StudyLaunchMode =
        requestedMode ?: DEFAULT_MODE_PRIORITY.firstOrNull { mode ->
            filteredWords(mode, words, recordsByWordId, nowProvider()).isNotEmpty()
        } ?: StudyLaunchMode.NEW_WORDS

    fun plan(
        mode: StudyLaunchMode,
        words: List<Word>,
        recordsByWordId: Map<Long, LearningRecord>,
        groupSize: Int,
    ): StudyQueuePlan {
        val filteredWords = filteredWords(mode, words, recordsByWordId, nowProvider())
        val queue = queueBuilder.buildFromWords(filteredWords.take(groupSize))
        return StudyQueuePlan(
            queue = queue,
            emptyState = if (queue.isEmpty()) mode.toEmptyState() else null,
        )
    }

    private fun StudyLaunchMode.toEmptyState(): StudyQueueEmptyState =
        when (this) {
            StudyLaunchMode.NEW_WORDS -> StudyQueueEmptyState.NO_NEW_WORDS
            StudyLaunchMode.REVIEW -> StudyQueueEmptyState.NO_DUE_REVIEW
            StudyLaunchMode.RECENT_MISTAKES -> StudyQueueEmptyState.NO_RECENT_MISTAKES
        }

    private fun filteredWords(
        mode: StudyLaunchMode,
        words: List<Word>,
        recordsByWordId: Map<Long, LearningRecord>,
        now: Instant,
    ): List<Word> =
        when (mode) {
            StudyLaunchMode.NEW_WORDS -> words.filter { word ->
                (recordsByWordId[word.id]?.reviewCount ?: 0) == 0
            }
            StudyLaunchMode.REVIEW -> words.filter { word ->
                recordsByWordId[word.id]?.nextReviewAt?.let { dueAt ->
                    !dueAt.isAfter(now)
                } == true
            }
            StudyLaunchMode.RECENT_MISTAKES -> words.filter { word ->
                recordsByWordId[word.id].isRecentMistake(now)
            }
        }

    private fun LearningRecord?.isRecentMistake(now: Instant): Boolean {
        val record = this ?: return false
        val lastReviewedAt = record.lastReviewedAt ?: return false
        if (record.lastOutcome?.lowercase() !in RECENT_MISTAKE_OUTCOMES) {
            return false
        }
        return !lastReviewedAt.isBefore(now.minus(RECENT_MISTAKE_WINDOW_DAYS, ChronoUnit.DAYS))
    }

    private companion object {
        val DEFAULT_MODE_PRIORITY = listOf(
            StudyLaunchMode.RECENT_MISTAKES,
            StudyLaunchMode.REVIEW,
            StudyLaunchMode.NEW_WORDS,
        )
        const val RECENT_MISTAKE_WINDOW_DAYS = 3L
        val RECENT_MISTAKE_OUTCOMES = setOf("not_known", "fuzzy", "wrong", "forgot", "again")
    }
}

fun defaultStudyGroupSize(mode: StudyLaunchMode): Int =
    when (mode) {
        StudyLaunchMode.NEW_WORDS -> 5
        StudyLaunchMode.REVIEW -> 20
        StudyLaunchMode.RECENT_MISTAKES -> 10
    }
