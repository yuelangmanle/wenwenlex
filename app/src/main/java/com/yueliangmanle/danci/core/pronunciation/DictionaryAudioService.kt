package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PronunciationAccent
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DictionaryAudioService(
    private val baseUrl: String = "https://api.dictionaryapi.dev/api/v2/entries/en/",
) {
    suspend fun resolveCandidate(
        word: String,
        accent: PronunciationAccent,
    ): DictionaryAudioCandidate? = withContext(Dispatchers.IO) {
        if (word.isBlank()) {
            return@withContext null
        }
        val requestUrl = baseUrl + URLEncoder.encode(word.trim(), "UTF-8")
        val connection = URL(requestUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val candidates = parseCandidates(body)
            pickCandidate(candidates, accent)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCandidates(body: String): List<DictionaryAudioCandidate> {
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

    private fun pickCandidate(
        candidates: List<DictionaryAudioCandidate>,
        accent: PronunciationAccent,
    ): DictionaryAudioCandidate? {
        if (candidates.isEmpty()) {
            return null
        }
        return candidates.firstOrNull { it.accent == accent }
            ?: candidates.firstOrNull { it.accent == PronunciationAccent.AUTO }
            ?: candidates.first()
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
}
