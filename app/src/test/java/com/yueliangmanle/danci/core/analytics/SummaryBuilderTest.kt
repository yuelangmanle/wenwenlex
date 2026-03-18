package com.yueliangmanle.danci.core.analytics

import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.WeeklySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryBuilderTest {
    @Test
    fun buildsSevenDayAndThirtyDaySlicesWithoutRawLogFlooding() {
        val payload = SummaryBuilder().buildContext(
            sevenDay = sampleDailySummaries(count = 7),
            thirtyDay = sampleWeeklySummaries(count = 4),
            rawEvents = sampleEvents(count = 500),
        )

        assertEquals(7, payload.dailyTrend.size)
        assertEquals(4, payload.weeklyTrend.size)
        assertTrue(payload.rawSamples.size <= 20)
        assertTrue(payload.dailyTrend.isNotEmpty())
    }

    private fun sampleDailySummaries(count: Int): List<DailySummary> =
        List(count) { index ->
            val date = LocalDate.parse("2026-03-12").plusDays(index.toLong())
            DailySummary(
                date = date.toString(),
                studiedCount = 20 + index,
                reviewCount = 10 + index,
                correctRate = 0.65f + (index * 0.02f),
                fatigueNote = if (index >= 5) "后段反应变慢" else null,
                primaryMistakeReasons = listOf("易混义项"),
                updatedAt = date.atStartOfDay().toInstant(ZoneOffset.UTC),
            )
        }

    private fun sampleWeeklySummaries(count: Int): List<WeeklySummary> =
        List(count) { index ->
            val weekStart = LocalDate.parse("2026-02-16").plusWeeks(index.toLong())
            WeeklySummary(
                weekStartDate = weekStart.toString(),
                studiedCount = 120 + (index * 8),
                correctRate = 0.6f + (index * 0.05f),
                trendSummary = "第 ${index + 1} 周保持推进",
                persistentWeakSpots = listOf("近义词辨析", "拼写相近词"),
                updatedAt = weekStart.atStartOfDay().toInstant(ZoneOffset.UTC),
            )
        }

    private fun sampleEvents(count: Int): List<StudyEvent> =
        List(count) { index ->
            StudyEvent(
                id = index.toLong() + 1L,
                wordId = (index % 20).toLong() + 1L,
                eventType = if (index % 3 == 0) "quiz_answered" else "card_feedback",
                feedback = if (index % 4 == 0) "wrong" else "known",
                isCorrect = index % 4 != 0,
                happenedAt = Instant.parse("2026-03-18T12:00:00Z").minusSeconds(index.toLong() * 90L),
                elapsedMillis = 2_000L + (index % 8) * 1_000L,
                metadata = if (index % 4 == 0) {
                    "mode=quiz&confusedWordId=${(index % 7) + 2}&relationType=confused_with"
                } else {
                    "mode=card"
                },
            )
        }
}
