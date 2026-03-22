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
}
