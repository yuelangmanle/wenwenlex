package com.yueliangmanle.danci.core.model

import java.time.Instant

data class VoicePack(
    val id: String,
    val name: String,
    val locale: String,
    val accent: String = PronunciationAccent.AUTO.storageValue,
    val engineType: String = VoicePackEngineType.SHERPA_ONNX.storageValue,
    val version: String = "1",
    val downloadUrl: String? = null,
    val manifestUrl: String? = null,
    val installDir: String? = null,
    val archiveChecksum: String? = null,
    val installedSizeBytes: Long = 0,
    val status: String = VoicePackStatus.NOT_INSTALLED.storageValue,
    val isActive: Boolean = false,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
)

