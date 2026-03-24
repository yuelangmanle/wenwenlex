package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.PronunciationAccent
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MiMoCloudTtsProvider(
    private val openConnection: (String) -> HttpURLConnection = { endpoint ->
        URL(endpoint).openConnection() as HttpURLConnection
    },
) : CloudTtsProvider {
    override val providerId: String = "mimo"
    override val providerLabel: String = "MiMo"

    override fun presets(): List<CloudTtsPreset> =
        listOf(
            CloudTtsPreset(
                id = "default_en",
                label = "英文女声",
                voice = "default_en",
            ),
            CloudTtsPreset(
                id = "default_zh",
                label = "中文女声",
                voice = "default_zh",
            ),
            CloudTtsPreset(
                id = "mimo_default",
                label = "通用默认声线",
                voice = "mimo_default",
            ),
        )

    override suspend fun synthesize(
        text: String,
        accent: PronunciationAccent,
        config: CloudTtsProviderConfig,
    ): CloudTtsSynthesisResult? = withContext(Dispatchers.IO) {
        val preset = resolvePreset(config.presetId)
        val rawBody = execute(
            endpoint = normalizeEndpoint(config.baseUrl),
            apiKey = config.apiKey,
            requestBody = buildRequestBody(
                text = buildPromptText(text, preset, accent),
                model = config.model,
                voice = preset.voice,
            ),
        )
        val json = JSONObject(rawBody)
        val audioData = json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optJSONObject("audio")
            ?.optString("data")
            .orEmpty()
        if (audioData.isBlank()) {
            return@withContext null
        }
        CloudTtsSynthesisResult(
            audioBytes = Base64.getDecoder().decode(audioData),
            mimeType = "audio/wav",
            providerLabel = providerLabel,
            presetLabel = preset.label,
        )
    }

    override suspend fun checkHealth(config: CloudTtsProviderConfig): CloudTtsHealthResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val preset = resolvePreset(config.presetId)
                execute(
                    endpoint = normalizeEndpoint(config.baseUrl),
                    apiKey = config.apiKey,
                    requestBody = buildRequestBody(
                        text = buildPromptText("health check", preset, PronunciationAccent.AUTO),
                        model = config.model,
                        voice = preset.voice,
                    ),
                )
                CloudTtsHealthResult(
                    healthy = true,
                    message = "MiMo 接口响应正常。",
                )
            }.getOrElse { error ->
                CloudTtsHealthResult(
                    healthy = false,
                    message = error.message ?: "MiMo 接口检测失败。",
                )
            }
        }

    private fun resolvePreset(presetId: String): CloudTtsPreset =
        presets().firstOrNull { it.id == presetId } ?: presets().first()

    private fun normalizeEndpoint(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) {
            trimmed
        } else {
            "$trimmed/chat/completions"
        }
    }

    private fun buildRequestBody(
        text: String,
        model: String,
        voice: String,
    ): String =
        JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "assistant")
                        .put("content", text),
                ),
            )
            .put(
                "audio",
                JSONObject()
                    .put("format", "wav")
                    .put("voice", voice),
            )
            .toString()

    private fun buildPromptText(
        text: String,
        preset: CloudTtsPreset,
        accent: PronunciationAccent,
    ): String =
        listOfNotNull(
            preset.promptPrefix,
            accentHint(accent),
            text.trim(),
        ).joinToString("\n")

    private fun accentHint(accent: PronunciationAccent): String? =
        when (accent) {
            PronunciationAccent.UK -> "Use a British English pronunciation."
            PronunciationAccent.US -> "Use an American English pronunciation."
            PronunciationAccent.AUTO -> null
        }

    private fun execute(
        endpoint: String,
        apiKey: String,
        requestBody: String,
    ): String {
        val connection = openConnection(endpoint).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doInput = true
            doOutput = true
            setRequestProperty("api-key", apiKey)
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            connection.outputStream.use { stream ->
                stream.write(requestBody.toByteArray(Charsets.UTF_8))
            }
            val rawBody = runCatching {
                val source = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                source?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("MiMo TTS 请求失败：HTTP ${connection.responseCode} $rawBody")
            }
            rawBody
        } finally {
            connection.disconnect()
        }
    }
}
