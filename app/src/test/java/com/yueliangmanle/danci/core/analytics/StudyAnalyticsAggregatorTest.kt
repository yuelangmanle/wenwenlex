package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.StudyEvent
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StudyAnalyticsAggregatorTest {
    @Test
    fun buildsConfusionGraphFromRepeatedMistakes() {
        val result = StudyAnalyticsAggregator().aggregate(
            events = listOf(
                mistakeEvent(happenedAt = "2026-03-18T08:00:00Z"),
                mistakeEvent(happenedAt = "2026-03-18T08:10:00Z"),
                correctEvent(happenedAt = "2026-03-18T08:20:00Z"),
            ),
            referenceTime = Instant.parse("2026-03-18T12:00:00Z"),
        )

        val edge = result.confusionEdges.single()
        assertEquals(1L, edge.sourceWordId)
        assertEquals(2L, edge.targetWordId)
        assertEquals("confused_with", edge.relationType)
        assertEquals(2, edge.mistakeCount)
        assertEquals(2f, edge.weight)
        assertFalse(result.dailySummaries.isEmpty())
    }

    private fun mistakeEvent(happenedAt: String): StudyEvent =
        StudyEvent(
            wordId = 1L,
            eventType = "quiz_answered",
            isCorrect = false,
            feedback = "wrong",
            happenedAt = Instant.parse(happenedAt),
            elapsedMillis = 11_000L,
            metadata = "mode=quiz&confusedWordId=2&relationType=confused_with",
        )

    private fun correctEvent(happenedAt: String): StudyEvent =
        StudyEvent(
            wordId = 1L,
            eventType = "quiz_answered",
            isCorrect = true,
            feedback = "correct",
            happenedAt = Instant.parse(happenedAt),
            elapsedMillis = 4_000L,
            metadata = "mode=quiz",
        )
}
