package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.LearningRecord
import com.yueliangmanle.danci.core.model.Word
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyQueuePlannerTest {

    @Test
    fun planner_returnsOnlyUnseenWordsForNewWordsMode() {
        val result = StudyQueuePlanner().plan(
            mode = StudyLaunchMode.NEW_WORDS,
            words = listOf(word(1), word(2), word(3)),
            recordsByWordId = mapOf(
                2L to record(2, familiarityState = "熟悉"),
                3L to record(3, familiarityState = "生疏"),
            ),
            groupSize = 5,
        )

        assertEquals(listOf(1L), result.queue.map { it.wordId })
    }

    @Test
    fun planner_returnsOnlyDueReviewWordsForReviewMode() {
        val now = Instant.parse("2026-03-24T10:00:00Z")
        val result = StudyQueuePlanner(nowProvider = { now }).plan(
            mode = StudyLaunchMode.REVIEW,
            words = listOf(word(1), word(2), word(3)),
            recordsByWordId = mapOf(
                1L to record(1, nextReviewAt = now.minusSeconds(60)),
                2L to record(2, nextReviewAt = now),
                3L to record(3, nextReviewAt = now.plusSeconds(60)),
            ),
            groupSize = 20,
        )

        assertEquals(listOf(1L, 2L), result.queue.map { it.wordId })
        assertEquals(null, result.emptyState)
    }

    @Test
    fun planner_returnsRecentMistakeWordsForRecentMistakesMode() {
        val now = Instant.parse("2026-03-24T10:00:00Z")
        val result = StudyQueuePlanner(nowProvider = { now }).plan(
            mode = StudyLaunchMode.RECENT_MISTAKES,
            words = listOf(word(1), word(2), word(3), word(4)),
            recordsByWordId = mapOf(
                1L to record(1, lastOutcome = "known"),
                2L to record(2, lastOutcome = "not_known", lastReviewedAt = now.minusSeconds(60)),
                3L to record(3, lastOutcome = "fuzzy", lastReviewedAt = now.minusSeconds(2 * 24 * 3600)),
                4L to record(4, lastOutcome = "fuzzy", lastReviewedAt = now.minusSeconds(5 * 24 * 3600)),
            ),
            groupSize = 10,
        )

        assertEquals(listOf(2L, 3L), result.queue.map { it.wordId })
        assertEquals(null, result.emptyState)
    }

    @Test
    fun planner_fallsBackToFirstAvailableModeWhenLaunchModeMissing() {
        val now = Instant.parse("2026-03-24T10:00:00Z")
        val planner = StudyQueuePlanner(nowProvider = { now })

        val resolvedMode = planner.resolveMode(
            requestedMode = null,
            words = listOf(word(1), word(2), word(3)),
            recordsByWordId = mapOf(
                1L to record(1, reviewCount = 1, lastOutcome = "not_known", lastReviewedAt = now.minusSeconds(60)),
                2L to record(2, reviewCount = 1, nextReviewAt = now.minusSeconds(60)),
            ),
        )

        assertEquals(StudyLaunchMode.RECENT_MISTAKES, resolvedMode)
    }

    @Test
    fun planner_returnsEmptyStateWhenNoDueReviewWordsExist() {
        val result = StudyQueuePlanner().plan(
            mode = StudyLaunchMode.REVIEW,
            words = listOf(word(1)),
            recordsByWordId = mapOf(1L to record(1, nextReviewAt = Instant.now().plusSeconds(3600))),
            groupSize = 20,
        )

        assertEquals(StudyQueueEmptyState.NO_DUE_REVIEW, result.emptyState)
    }

    private fun word(id: Long): Word =
        Word(
            id = id,
            lemma = "word$id",
            meanings = listOf("meaning$id"),
        )

    private fun record(
        wordId: Long,
        familiarityState: String = "未学",
        nextReviewAt: Instant? = null,
        reviewCount: Int = 1,
        lastOutcome: String? = null,
        lastReviewedAt: Instant? = null,
    ): LearningRecord =
        LearningRecord(
            wordId = wordId,
            mastery = 0.5f,
            familiarityState = familiarityState,
            nextReviewAt = nextReviewAt,
            reviewCount = reviewCount,
            lastReviewedAt = lastReviewedAt,
            lastOutcome = lastOutcome,
        )
}
