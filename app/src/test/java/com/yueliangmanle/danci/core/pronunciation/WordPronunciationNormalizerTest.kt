package com.yueliangmanle.danci.core.pronunciation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordPronunciationNormalizerTest {
    @Test
    fun normalizerLowercasesAndPreservesSupportedApostrophes() {
        assertEquals("don't", normalizeWordForPronunciation(" Don't "))
    }

    @Test
    fun normalizerRejectsUnsupportedCharacters() {
        assertNull(normalizeWordForPronunciation("can't!"))
    }
}
