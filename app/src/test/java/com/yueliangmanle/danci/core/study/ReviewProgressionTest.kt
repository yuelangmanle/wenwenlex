package com.yueliangmanle.danci.core.study

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewProgressionTest {

    @Test
    fun advance_initialPassSchedulesTenMinuteReview() {
        val next = ReviewProgression().advance(
            currentStage = null,
            outcome = ReviewOutcome.PASS,
            now = Instant.parse("2026-03-23T00:00:00Z"),
        )

        assertEquals(0, next.stage)
        assertEquals(Instant.parse("2026-03-23T00:10:00Z"), next.nextReviewAt)
    }

    @Test
    fun advance_passFromTenMinuteStagePromotesToOneDay() {
        val next = ReviewProgression().advance(
            currentStage = 0,
            outcome = ReviewOutcome.PASS,
            now = Instant.parse("2026-03-23T00:00:00Z"),
        )

        assertEquals(1, next.stage)
        assertEquals(Instant.parse("2026-03-24T00:00:00Z"), next.nextReviewAt)
    }

    @Test
    fun advance_fuzzyRegressesOneStage() {
        val next = ReviewProgression().advance(
            currentStage = 3,
            outcome = ReviewOutcome.FUZZY,
            now = Instant.parse("2026-03-23T00:00:00Z"),
        )

        assertEquals(2, next.stage)
        assertEquals(Instant.parse("2026-03-26T00:00:00Z"), next.nextReviewAt)
    }

    @Test
    fun advance_failRegressesTwoStages() {
        val next = ReviewProgression().advance(
            currentStage = 4,
            outcome = ReviewOutcome.FAIL,
            now = Instant.parse("2026-03-23T00:00:00Z"),
        )

        assertEquals(2, next.stage)
        assertEquals(Instant.parse("2026-03-26T00:00:00Z"), next.nextReviewAt)
    }
}
