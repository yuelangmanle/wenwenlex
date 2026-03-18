package com.yueliangmanle.danci.core.ai

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.model.AiCapability
import org.junit.Assert.assertEquals
import org.junit.Test

class AiProfileResolverTest {
    private val resolver = AiProfileResolver()

    @Test
    fun phoneticFillFallsBackToDefaultProfileWhenNoOverrideExists() {
        val settings = AppSettings(
            defaultAiProfileId = "default",
            phoneticFillProfileId = null,
        )

        val resolved = resolver.resolveProfileId(settings, AiCapability.PHONETIC_FILL)

        assertEquals("default", resolved)
    }

    @Test
    fun capabilityOverrideTakesPriorityOverDefault() {
        val settings = AppSettings(
            defaultAiProfileId = "default",
            wordHelpProfileId = "word-help",
        )

        val resolved = resolver.resolveProfileId(settings, AiCapability.WORD_HELP)

        assertEquals("word-help", resolved)
    }
}
