package com.yueliangmanle.danci.core.worker

import java.io.File
import java.security.MessageDigest

class StreamingSha256(
    private val bufferSize: Int = DEFAULT_STREAM_BUFFER_SIZE,
) {
    fun checksum(file: File): String =
        file.inputStream().buffered().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(bufferSize)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
}

internal const val DEFAULT_STREAM_BUFFER_SIZE = 64 * 1024
