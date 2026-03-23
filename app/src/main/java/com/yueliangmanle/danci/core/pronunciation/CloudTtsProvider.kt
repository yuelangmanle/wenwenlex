package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.AiProviderProfile
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CloudTtsRequest(
    val profile: AiProviderProfile,
    val apiKey: String,
    val text: String,
    val voice: String,
    val audioFormat: String = "wav",
    val style: String? = null,
)

data class CloudTtsResult(
    val audioBytes: ByteArray,
    val mimeType: String,
    val latencyMs: Long,
    val rawBody: String,
)

data class CloudTtsHealthCheckResult(
    val success: Boolean,
    val summary: String,
    val latencyMs: Long? = null,
    val statusCode: Int? = null,
    val audioBytesLength: Int? = null,
)

data class CloudTtsHttpResponse(
    val statusCode: Int,
    val body: String,
)

interface CloudTtsTransport {
    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): CloudTtsHttpResponse
}

class HttpUrlConnectionCloudTtsTransport : CloudTtsTransport {
    override suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): CloudTtsHttpResponse = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.doInput = true
            connection.doOutput = true
            headers.forEach { (name, value) ->
                connection.setRequestProperty(name, value)
            }
            connection.outputStream.use { stream ->
                stream.write(body.toByteArray(Charsets.UTF_8))
            }
            val responseBody = runCatching {
                val source = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                source?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            CloudTtsHttpResponse(
                statusCode = connection.responseCode,
                body = responseBody,
            )
        } finally {
            connection.disconnect()
        }
    }
}

interface CloudTtsProvider {
    suspend fun synthesize(request: CloudTtsRequest): CloudTtsResult

    suspend fun healthCheck(
        profile: AiProviderProfile,
        apiKey: String,
        voice: String,
        text: String = "Hello, this is a pronunciation health check.",
        style: String? = "Calm",
    ): CloudTtsHealthCheckResult
}
