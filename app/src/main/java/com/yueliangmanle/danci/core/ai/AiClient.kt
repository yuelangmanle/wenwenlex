package com.yueliangmanle.danci.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiRuntimeSettings(
    val enabled: Boolean,
    val baseUrl: String,
    val apiKey: String? = null,
    val model: String,
)

data class AiTextRequest(
    val runtimeSettings: AiRuntimeSettings,
    val instructions: String,
    val input: String,
    val responseFormat: AiResponseFormat = AiResponseFormat.PlainText,
)

data class AiTextResponse(
    val text: String,
    val rawBody: String,
)

sealed interface AiResponseFormat {
    data object PlainText : AiResponseFormat

    data class JsonSchema(
        val name: String,
        val schema: JSONObject,
        val strict: Boolean = true,
    ) : AiResponseFormat
}

interface AiClient {
    suspend fun generate(request: AiTextRequest): AiTextResponse
}

class ResponsesApiAiClient : AiClient {
    override suspend fun generate(request: AiTextRequest): AiTextResponse = withContext(Dispatchers.IO) {
        val endpoint = normalizeResponsesEndpoint(request.runtimeSettings.baseUrl)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doInput = true
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${request.runtimeSettings.apiKey.orEmpty()}")
            setRequestProperty("Content-Type", "application/json")
        }

        connection.outputStream.use { stream ->
            stream.write(buildRequestBody(request).toByteArray(Charsets.UTF_8))
        }

        val rawBody = runCatching {
            val source = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            source?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrDefault("")

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("AI request failed with ${connection.responseCode}: $rawBody")
        }

        AiTextResponse(
            text = extractText(rawBody),
            rawBody = rawBody,
        )
    }

    private fun buildRequestBody(request: AiTextRequest): String =
        JSONObject().apply {
            put("model", request.runtimeSettings.model)
            put("instructions", request.instructions)
            put("input", request.input)
            when (val format = request.responseFormat) {
                AiResponseFormat.PlainText -> Unit
                is AiResponseFormat.JsonSchema -> {
                    put(
                        "text",
                        JSONObject().put(
                            "format",
                            JSONObject()
                                .put("type", "json_schema")
                                .put("name", format.name)
                                .put("schema", format.schema)
                                .put("strict", format.strict),
                        ),
                    )
                }
            }
        }.toString()

    private fun extractText(rawBody: String): String {
        val json = JSONObject(rawBody)
        json.optString("output_text")
            .takeIf(String::isNotBlank)
            ?.let { return it }
        val output = json.optJSONArray("output") ?: return rawBody
        return findOutputText(output)?.takeIf(String::isNotBlank) ?: rawBody
    }

    private fun findOutputText(output: JSONArray): String? {
        for (index in 0 until output.length()) {
            val item = output.optJSONObject(index) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val block = content.optJSONObject(contentIndex) ?: continue
                val text = when (block.optString("type")) {
                    "output_text" -> block.optString("text")
                    "text" -> block.optString("text")
                    else -> ""
                }
                if (text.isNotBlank()) {
                    return text
                }
            }
        }
        return null
    }

    private fun normalizeResponsesEndpoint(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/responses")) trimmed else "$trimmed/responses"
    }
}
