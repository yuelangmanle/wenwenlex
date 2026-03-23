package com.yueliangmanle.danci.core.worker

import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject

data class VoicePackRemoteStream(
    val inputStream: java.io.InputStream,
    val responseCode: Int,
    val supportsResume: Boolean,
    val contentLength: Long? = null,
)

data class VoicePackArchiveDownloadResult(
    val checksum: String,
    val resumedFromBytes: Long,
    val downloadedBytes: Long,
)

internal class VoicePackArchiveDownloader(
    private val remoteFetcher: VoicePackRemoteFetcher,
    private val streamingSha256: StreamingSha256 = StreamingSha256(),
) {
    fun download(
        remoteUrl: String,
        targetFile: File,
    ): VoicePackArchiveDownloadResult {
        val partFile = File("${targetFile.absolutePath}.part")
        val metaFile = File("${targetFile.absolutePath}.meta")
        val initialBytes = partFile.takeIf(File::exists)?.length() ?: 0L
        var resumedFromBytes = initialBytes
        var remoteStream = remoteFetcher.openStream(remoteUrl, initialBytes)
        var append = initialBytes > 0 && remoteStream.supportsResume && remoteStream.responseCode == 206

        if (initialBytes > 0 && !append) {
            partFile.delete()
            metaFile.delete()
            resumedFromBytes = 0L
            remoteStream = remoteFetcher.openStream(remoteUrl, 0L)
            append = false
        }

        partFile.parentFile?.mkdirs()
        remoteStream.inputStream.use { input ->
            FileOutputStream(partFile, append).use { output ->
                val buffer = ByteArray(DEFAULT_STREAM_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) {
                        break
                    }
                    output.write(buffer, 0, read)
                }
                output.fd.sync()
            }
        }

        partFile.copyTo(targetFile, overwrite = true)
        val checksum = streamingSha256.checksum(targetFile)
        metaFile.writeText(
            JSONObject().apply {
                put("url", remoteUrl)
                put("resumedFromBytes", resumedFromBytes)
                put("archiveBytes", targetFile.length())
            }.toString(),
        )
        return VoicePackArchiveDownloadResult(
            checksum = checksum,
            resumedFromBytes = resumedFromBytes,
            downloadedBytes = targetFile.length() - resumedFromBytes,
        )
    }
}
