package com.yueliangmanle.danci.core.study

import com.yueliangmanle.danci.core.model.Word
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyQueueBuilderTest {
    @Test
    fun buildFromWords_applies_ranked_queue_buckets() {
        val queue = StudyQueueBuilder().buildFromWords(
            words = listOf(
                Word(id = 1L, lemma = "abandon"),
                Word(id = 2L, lemma = "ability"),
                Word(id = 3L, lemma = "able"),
            ),
            queueBuckets = mapOf(
                1L to "rescue",
                2L to "review",
            ),
        )

        assertEquals(listOf("rescue", "review", "new"), queue.map(StudyCardItem::queueBucket))
    }
}
