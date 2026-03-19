package com.yueliangmanle.danci.core.data

import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.time.Instant

object TestVoicePackFactory {
    fun voicePack(
        id: String,
        name: String = id,
        locale: String = "en-US",
        accent: String = PronunciationAccent.AUTO.storageValue,
        engineType: String = VoicePackEngineType.SHERPA_ONNX.storageValue,
        version: String = "1",
        downloadUrl: String? = "asset://pronunciation/packs/$id",
        installDir: String? = null,
        installedSizeBytes: Long = 0L,
        status: String = VoicePackStatus.NOT_INSTALLED.storageValue,
        isActive: Boolean = false,
        now: Instant = Instant.parse("2026-03-19T12:00:00Z"),
    ): VoicePack =
        VoicePack(
            id = id,
            name = name,
            locale = locale,
            accent = accent,
            engineType = engineType,
            version = version,
            downloadUrl = downloadUrl,
            installDir = installDir,
            installedSizeBytes = installedSizeBytes,
            status = status,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
}
