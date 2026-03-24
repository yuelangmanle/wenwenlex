package com.yueliangmanle.danci.core.study

import org.junit.Assert.assertEquals
import org.junit.Test

class WordPassPolicyTest {
    @Test
    fun wordPassPolicy_requiresRecallBeforeSpellWhenSpellingEnabled() {
        val policy = WordPassPolicy(spellingEnabled = true)

        val afterMeaning = policy.advance(WordPassStep.MEANING, passed = true)
        assertEquals(WordPassStep.RECALL, afterMeaning.nextStep)

        val afterRecall = policy.advance(afterMeaning.nextStep, passed = true)
        assertEquals(WordPassStep.SPELLING, afterRecall.nextStep)

        val afterSpelling = policy.advance(afterRecall.nextStep, passed = true)
        assertEquals(WordPassStep.DONE, afterSpelling.nextStep)
    }

    @Test
    fun wordPassPolicy_skipsSpellingWhenDisabled() {
        val policy = WordPassPolicy(spellingEnabled = false)

        val afterMeaning = policy.advance(WordPassStep.MEANING, passed = true)
        val afterRecall = policy.advance(afterMeaning.nextStep, passed = true)

        assertEquals(WordPassStep.DONE, afterRecall.nextStep)
    }

    @Test
    fun wordPassPolicy_resetsToMeaningWhenCurrentStepFails() {
        val policy = WordPassPolicy(spellingEnabled = true)

        val afterFailure = policy.advance(WordPassStep.RECALL, passed = false)

        assertEquals(WordPassStep.MEANING, afterFailure.nextStep)
    }
}
