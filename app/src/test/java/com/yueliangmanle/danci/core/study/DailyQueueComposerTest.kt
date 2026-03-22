package com.yueliangmanle.danci.core.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyQueueComposerTest {
    @Test
    fun compose_limits_new_words_when_rescue_bucket_is_heavy() {
        val ranked = buildList {
            repeat(12) { index ->
                add(
                    RankedReviewItem(
                        wordId = index + 1L,
                        priorityScore = 100f - index,
                        bucket = "rescue",
                    ),
                )
            }
            repeat(18) { index ->
                add(
                    RankedReviewItem(
                        wordId = index + 101L,
                        priorityScore = 60f - index,
                        bucket = "review",
                    ),
                )
            }
        }

        val plan = DailyQueueComposer().compose(
            goal = 30,
            ranked = ranked,
            unseenWords = 200,
        )

        assertTrue(plan.rescueCount > 0)
        assertTrue(plan.newWordCount < plan.reviewCount)
        assertEquals("今天先稳住 ${plan.rescueCount} 个高风险词", plan.queueHeadline)
    }

    @Test
    fun compose_caps_review_count_within_daily_goal_when_urgent_reviews_overflow() {
        val ranked = List(10) { index ->
            RankedReviewItem(
                wordId = index + 1L,
                priorityScore = 55f - index,
                bucket = "review",
                isDueToday = true,
            )
        }

        val plan = DailyQueueComposer().compose(
            goal = 5,
            ranked = ranked,
            unseenWords = 20,
        )

        assertEquals(5, plan.reviewCount)
        assertEquals(0, plan.rescueCount)
        assertEquals(0, plan.newWordCount)
        assertEquals(5, plan.rescueCount + plan.reviewCount + plan.newWordCount)
    }
}
