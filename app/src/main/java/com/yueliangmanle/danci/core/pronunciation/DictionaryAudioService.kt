package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PronunciationAccent
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

private const val DEFAULT_DICTIONARY_API_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/"

open class DictionaryAudioService(
    baseUrl: String = DEFAULT_DICTIONARY_API_BASE_URL,
    providers: List<DictionaryAudioProvider>? = null,
) {
    private val providers: List<DictionaryAudioProvider> = providers ?: defaultDictionaryAudioProviders(baseUrl)

    open suspend fun resolveCandidates(
        word: String,
        accent: PronunciationAccent,
    ): List<DictionaryAudioCandidate> = withContext(Dispatchers.IO) {
        val normalizedWord = word.trim()
        if (normalizedWord.isBlank()) {
            return@withContext emptyList()
        }

        providers
            .flatMapIndexed { providerIndex, provider ->
                runCatching { provider.resolveCandidates(normalizedWord) }
                    .getOrDefault(emptyList())
                    .mapIndexed { candidateIndex, candidate ->
                        RankedDictionaryAudioCandidate(
                            candidate = candidate.copy(
                                sourceLabel = candidate.sourceLabel ?: provider.label,
                            ),
                            providerIndex = providerIndex,
                            candidateIndex = candidateIndex,
                        )
                    }
            }
            .sortedWith(
                compareBy<RankedDictionaryAudioCandidate> {
                    accentPriority(
                        requestedAccent = accent,
                        candidateAccent = it.candidate.accent,
                    )
                }
                    .thenBy(RankedDictionaryAudioCandidate::providerIndex)
                    .thenBy(RankedDictionaryAudioCandidate::candidateIndex),
            )
            .map(RankedDictionaryAudioCandidate::candidate)
            .distinctBy(DictionaryAudioCandidate::url)
    }

    open suspend fun resolveCandidate(
        word: String,
        accent: PronunciationAccent,
    ): DictionaryAudioCandidate? = resolveCandidates(word, accent).firstOrNull()
}

interface DictionaryAudioProvider {
    val label: String

    suspend fun resolveCandidates(word: String): List<DictionaryAudioCandidate>
}

private data class RankedDictionaryAudioCandidate(
    val candidate: DictionaryAudioCandidate,
    val providerIndex: Int,
    val candidateIndex: Int,
)

private class DictionaryApiDevAudioProvider(
    private val baseUrl: String,
) : DictionaryAudioProvider {
    override val label: String = "Dictionary API"

    override suspend fun resolveCandidates(word: String): List<DictionaryAudioCandidate> = withContext(Dispatchers.IO) {
        val requestUrl = baseUrl + URLEncoder.encode(word, "UTF-8")
        val connection = URL(requestUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                return@withContext emptyList()
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseDictionaryApiCandidates(body)
        } finally {
            connection.disconnect()
        }
    }
}

private object YoudaoDictionaryAudioProvider : DictionaryAudioProvider {
    override val label: String = "有道词典"

    override suspend fun resolveCandidates(word: String): List<DictionaryAudioCandidate> {
        val encodedWord = URLEncoder.encode(word, "UTF-8")
        return listOf(
            DictionaryAudioCandidate(
                url = "https://dict.youdao.com/dictvoice?audio=$encodedWord&type=1",
                accent = PronunciationAccent.UK,
                mimeType = "audio/mpeg",
            ),
            DictionaryAudioCandidate(
                url = "https://dict.youdao.com/dictvoice?audio=$encodedWord&type=2",
                accent = PronunciationAccent.US,
                mimeType = "audio/mpeg",
            ),
        )
    }
}

private fun defaultDictionaryAudioProviders(baseUrl: String): List<DictionaryAudioProvider> =
    listOf(
        DictionaryApiDevAudioProvider(baseUrl),
        YoudaoDictionaryAudioProvider,
    )

private fun parseDictionaryApiCandidates(body: String): List<DictionaryAudioCandidate> {
    val root = JSONArray(body)
    val candidates = mutableListOf<DictionaryAudioCandidate>()
    repeat(root.length()) { entryIndex ->
        val entry = root.optJSONObject(entryIndex) ?: return@repeat
        val phonetics = entry.optJSONArray("phonetics") ?: JSONArray()
        repeat(phonetics.length()) { phoneticIndex ->
            val item = phonetics.optJSONObject(phoneticIndex) ?: return@repeat
            val audio = item.optString("audio").orEmpty().trim()
            if (audio.isBlank()) {
                return@repeat
            }
            val normalizedUrl = if (audio.startsWith("//")) {
                "https:$audio"
            } else {
                audio
            }
            candidates += DictionaryAudioCandidate(
                url = normalizedUrl,
                accent = guessAccent(
                    url = normalizedUrl,
                    text = item.optString("text"),
                ),
                mimeType = "audio/mpeg",
            )
        }
    }
    return candidates
}

private fun accentPriority(
    requestedAccent: PronunciationAccent,
    candidateAccent: PronunciationAccent,
): Int =
    when {
        requestedAccent == PronunciationAccent.AUTO && candidateAccent == PronunciationAccent.AUTO -> 0
        candidateAccent == requestedAccent -> 0
        candidateAccent == PronunciationAccent.AUTO -> 1
        requestedAccent == PronunciationAccent.AUTO -> 1
        else -> 2
    }

private fun guessAccent(
    url: String,
    text: String?,
): PronunciationAccent {
    val normalizedUrl = url.lowercase()
    val normalizedText = text.orEmpty().lowercase()
    return when {
        "uk" in normalizedUrl || "br" in normalizedText -> PronunciationAccent.UK
        "us" in normalizedUrl || "n_am" in normalizedText || "am" in normalizedText -> PronunciationAccent.US
        else -> PronunciationAccent.AUTO
    }
}
