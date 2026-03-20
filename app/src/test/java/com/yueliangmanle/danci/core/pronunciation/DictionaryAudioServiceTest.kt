package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PronunciationAccent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryAudioServiceTest {
    @Test
    fun resolveCandidatesPrefersExactAccentAcrossProvidersThenFallsBack() = runTest {
        val service = DictionaryAudioService(
            providers = listOf(
                FakeDictionaryAudioProvider(
                    label = "源A",
                    candidates = listOf(
                        DictionaryAudioCandidate(
                            url = "https://source-a.example/auto.mp3",
                            accent = PronunciationAccent.AUTO,
                        ),
                    ),
                ),
                FakeDictionaryAudioProvider(
                    label = "有道词典",
                    candidates = listOf(
                        DictionaryAudioCandidate(
                            url = "https://source-b.example/uk.mp3",
                            accent = PronunciationAccent.UK,
                        ),
                        DictionaryAudioCandidate(
                            url = "https://source-b.example/us.mp3",
                            accent = PronunciationAccent.US,
                        ),
                    ),
                ),
            ),
        )

        val candidates = service.resolveCandidates(
            word = "abandon",
            accent = PronunciationAccent.UK,
        )

        assertEquals(
            listOf(
                "https://source-b.example/uk.mp3",
                "https://source-a.example/auto.mp3",
                "https://source-b.example/us.mp3",
            ),
            candidates.map(DictionaryAudioCandidate::url),
        )
        assertEquals("有道词典", candidates.first().sourceLabel)
    }
}

private class FakeDictionaryAudioProvider(
    override val label: String,
    private val candidates: List<DictionaryAudioCandidate>,
) : DictionaryAudioProvider {
    override suspend fun resolveCandidates(word: String): List<DictionaryAudioCandidate> = candidates
}
