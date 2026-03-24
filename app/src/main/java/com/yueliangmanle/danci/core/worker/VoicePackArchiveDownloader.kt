package com.yueliangmanle.danci.core.worker

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ArchiveDownloadRequest(
    val url: String,
    val rangeStart: Long? = null,
)

internal data class ArchiveDownloadResponse(
    val responseCode: Int,
    val body: ByteArray,
)

internal data class VoicePackArchiveDownloadResult(
    val success: Boolean,
    val resumed: Boolean = false,
    val checksum: String? = null,
    val usedUrl: String? = null,
    val failureCode: String? = null,
    val failureMessage: String? = null,
    val nextSuggestedUrl: String? = null,
    val cancelled: Boolean = false,
)

internal class VoicePackArchiveDownloader(
    private val fetch: suspend (ArchiveDownloadRequest) -> ArchiveDownloadResponse = ::fetchArchiveDownloadResponse,
    private val shouldCancel: () -> Boolean = { false },
) {
    suspend fun download(
        urls: List<String>,
        targetFile: File,
    ): VoicePackArchiveDownloadResult {
        val orderedUrls = urls
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        if (orderedUrls.isEmpty()) {
            return VoicePackArchiveDownloadResult(
                success = false,
                failureCode = "source_missing",
                failureMessage = "语音包缺少下载地址。",
            )
        }
        if (shouldCancel()) {
            return VoicePackArchiveDownloadResult(success = false, cancelled = true)
        }

        val activeUrl = orderedUrls.first()
        val nextSuggestedUrl = orderedUrls.drop(1).firstOrNull()
        val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
        val existingBytes = partFile.takeIf(File::exists)?.length() ?: 0L

        val response = try {
            fetch(
                ArchiveDownloadRequest(
                    url = activeUrl,
                    rangeStart = existingBytes.takeIf { it > 0L },
                ),
            )
        } catch (error: SocketTimeoutException) {
            return VoicePackArchiveDownloadResult(
                success = false,
                usedUrl = activeUrl,
                failureCode = "source_timeout",
                failureMessage = "语音包下载超时，请稍后重试。",
                nextSuggestedUrl = nextSuggestedUrl,
            )
        } catch (error: Throwable) {
            return VoicePackArchiveDownloadResult(
                success = false,
                usedUrl = activeUrl,
                failureCode = "source_io_error",
                failureMessage = error.message ?: "语音包下载失败，请稍后重试。",
                nextSuggestedUrl = nextSuggestedUrl,
            )
        }

        if (response.responseCode !in 200..299) {
            return VoicePackArchiveDownloadResult(
                success = false,
                usedUrl = activeUrl,
                failureCode = mapHttpFailureCode(response.responseCode),
                failureMessage = "语音包下载失败：HTTP ${response.responseCode}",
                nextSuggestedUrl = nextSuggestedUrl,
            )
        }

        val resumed = existingBytes > 0L && response.responseCode == HttpURLConnection.HTTP_PARTIAL
        if (shouldCancel()) {
            return VoicePackArchiveDownloadResult(success = false, cancelled = true)
        }

        partFile.parentFile?.mkdirs()
        FileOutputStream(partFile, resumed).use { output ->
            if (!resumed && partFile.exists() && existingBytes > 0L) {
                output.channel.truncate(0)
            }
            output.write(response.body)
        }

        if (shouldCancel()) {
            return VoicePackArchiveDownloadResult(success = false, cancelled = true)
        }

        targetFile.parentFile?.mkdirs()
        if (targetFile.exists()) {
            targetFile.delete()
        }
        partFile.copyTo(targetFile, overwrite = true)
        partFile.delete()

        return VoicePackArchiveDownloadResult(
            success = true,
            resumed = resumed,
            checksum = sha256(targetFile.readBytes()),
            usedUrl = activeUrl,
        )
    }
}

private suspend fun fetchArchiveDownloadResponse(
    request: ArchiveDownloadRequest,
): ArchiveDownloadResponse = withContext(Dispatchers.IO) {
    val connection = URL(request.url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        request.rangeStart?.takeIf { it > 0L }?.let { rangeStart ->
            connection.setRequestProperty("Range", "bytes=$rangeStart-")
        }
        connection.connect()
        val body = runCatching<ByteArray> {
            val source = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            source?.use { input -> input.readBytes() } ?: byteArrayOf()
        }.getOrDefault(byteArrayOf())
        ArchiveDownloadResponse(
            responseCode = connection.responseCode,
            body = body,
        )
    } finally {
        connection.disconnect()
    }
}

private fun mapHttpFailureCode(responseCode: Int): String =
    when (responseCode) {
        HttpURLConnection.HTTP_NOT_FOUND -> "source_not_found"
        HttpURLConnection.HTTP_FORBIDDEN -> "source_forbidden"
        in 500..599 -> "source_server_error"
        else -> "source_http_error"
    }

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
