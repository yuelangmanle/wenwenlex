package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.Word
import java.time.Instant

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
    fun plan(
        mode: StudyLaunchMode,
        words: List<Word>,
        recordsByWordId: Map<Long, LearningRecord>,
        groupSize: Int,
    ): StudyQueuePlan {
        val filteredWords = when (mode) {
            StudyLaunchMode.NEW_WORDS -> words.filter { word ->
                (recordsByWordId[word.id]?.reviewCount ?: 0) == 0
            }
            StudyLaunchMode.REVIEW -> {
                val now = nowProvider()
                words.filter { word ->
                    recordsByWordId[word.id]?.nextReviewAt?.let { dueAt ->
                        !dueAt.isAfter(now)
                    } == true
                }
            }
            StudyLaunchMode.RECENT_MISTAKES -> words.filter { word ->
                recordsByWordId[word.id]?.lastOutcome in RECENT_MISTAKE_OUTCOMES
            }
        }
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

    private companion object {
        val RECENT_MISTAKE_OUTCOMES = setOf("not_known", "fuzzy")
    }
}

fun defaultStudyGroupSize(mode: StudyLaunchMode): Int =
    when (mode) {
        StudyLaunchMode.NEW_WORDS -> 5
        StudyLaunchMode.REVIEW -> 20
        StudyLaunchMode.RECENT_MISTAKES -> 10
    }
