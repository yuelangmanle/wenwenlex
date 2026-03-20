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
        checksumsUrl: String? = null,
        archiveChecksum: String? = null,
        installDir: String? = null,
        installedSizeBytes: Long = 0L,
        status: String = VoicePackStatus.NOT_INSTALLED.storageValue,
        isActive: Boolean = false,
        engineFamily: String? = null,
        modelFamily: String? = null,
        supportsImportedWords: Boolean = false,
        estimatedStorageBytes: Long? = null,
        estimatedRamMb: Int? = null,
        licenses: List<String> = emptyList(),
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
            checksumsUrl = checksumsUrl,
            archiveChecksum = archiveChecksum,
            installDir = installDir,
            installedSizeBytes = installedSizeBytes,
            status = status,
            isActive = isActive,
            engineFamily = engineFamily,
            modelFamily = modelFamily,
            supportsImportedWords = supportsImportedWords,
            estimatedStorageBytes = estimatedStorageBytes,
            estimatedRamMb = estimatedRamMb,
            licenses = licenses,
            createdAt = now,
            updatedAt = now,
        )
}
