package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.model.MIMO_TTS_BASE_URL
import com.yueliangmanle.danci.core.model.MIMO_TTS_MODEL
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MiMoTtsProvider(
    private val transport: CloudTtsTransport = HttpUrlConnectionCloudTtsTransport(),
    private val nanoTimeProvider: () -> Long = System::nanoTime,
) : CloudTtsProvider {
    override suspend fun synthesize(request: CloudTtsRequest): CloudTtsResult = withContext(Dispatchers.IO) {
        val requestBody = buildRequestBody(request)
        val startedAt = nanoTimeProvider()
        val response = transport.postJson(
            url = normalizeEndpoint(request.profile.baseUrl),
            headers = buildHeaders(request.apiKey),
            body = requestBody,
        )
        val latencyMs = (nanoTimeProvider() - startedAt) / 1_000_000
        check(response.statusCode in 200..299) {
            "MiMo TTS 请求失败：HTTP ${response.statusCode} ${response.body}"
        }
        val audioBytes = parseAudioBytes(response.body)
        CloudTtsResult(
            audioBytes = audioBytes,
            mimeType = mimeTypeForFormat(request.audioFormat),
            latencyMs = latencyMs,
            rawBody = response.body,
        )
    }

    override suspend fun healthCheck(
        profile: AiProviderProfile,
        apiKey: String,
        voice: String,
        text: String,
        style: String?,
    ): CloudTtsHealthCheckResult =
        runCatching {
            val result = synthesize(
                CloudTtsRequest(
                    profile = profile,
                    apiKey = apiKey,
                    text = text,
                    voice = voice,
                    style = style,
                ),
            )
            CloudTtsHealthCheckResult(
                success = true,
                summary = "鉴权成功，模型可用，已返回 ${result.audioBytes.size} 字节音频。",
                latencyMs = result.latencyMs,
                statusCode = 200,
                audioBytesLength = result.audioBytes.size,
            )
        }.getOrElse { error ->
            val message = error.message ?: "MiMo TTS 检测失败。"
            val statusCode = Regex("""HTTP\s+(\d{3})""")
                .find(message)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
            CloudTtsHealthCheckResult(
                success = false,
                summary = message,
                statusCode = statusCode,
            )
        }

    internal fun buildRequestBody(request: CloudTtsRequest): String =
        JSONObject().apply {
            put("model", MIMO_TTS_MODEL)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "assistant")
                        put("content", buildAssistantContent(request.text, request.style))
                    },
                ),
            )
            put(
                "audio",
                JSONObject().apply {
                    put("format", request.audioFormat.ifBlank { "wav" })
                    put("voice", request.voice.ifBlank { "default_en" })
                },
            )
        }.toString()

    private fun buildHeaders(apiKey: String): Map<String, String> =
        linkedMapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json",
        )

    private fun parseAudioBytes(rawBody: String): ByteArray {
        val json = JSONObject(rawBody)
        val choices = json.optJSONArray("choices") ?: JSONArray()
        for (index in 0 until choices.length()) {
            val choice = choices.optJSONObject(index) ?: continue
            val message = choice.optJSONObject("message") ?: continue
            val audio = message.optJSONObject("audio") ?: continue
            val data = audio.optString("data").trim()
            if (data.isNotEmpty()) {
                return Base64.getDecoder().decode(data)
            }
        }
        error("MiMo TTS 响应里没有可用音频数据。")
    }

    private fun buildAssistantContent(
        text: String,
        style: String?,
    ): String {
        val normalizedText = text.trim()
        require(normalizedText.isNotEmpty()) { "待合成文本不能为空。" }
        val normalizedStyle = style?.trim().orEmpty()
        return if (normalizedStyle.isEmpty()) {
            normalizedText
        } else {
            "<style>$normalizedStyle</style>$normalizedText"
        }
    }

    private fun normalizeEndpoint(baseUrl: String): String {
        val trimmed = baseUrl.trim().ifEmpty { MIMO_TTS_BASE_URL }.trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) {
            trimmed
        } else {
            "$trimmed/chat/completions"
        }
    }

    private fun mimeTypeForFormat(format: String): String =
        when (format.trim().lowercase()) {
            "mp3" -> "audio/mpeg"
            "pcm", "pcm16" -> "audio/pcm"
            else -> "audio/wav"
        }
}
