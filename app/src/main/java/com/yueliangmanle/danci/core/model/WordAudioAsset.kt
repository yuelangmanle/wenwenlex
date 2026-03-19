package com.yueliangmanle.danci.core.model

import java.time.Instant

data class WordAudioAsset(
    val id: Long = 0,
    val wordId: Long,
    val accent: String = PronunciationAccent.AUTO.storageValue,
    val sourceType: String = PlaybackSource.DICTIONARY_CACHE.storageValue,
    val remoteUrl: String? = null,
    val localPath: String? = null,
    val mimeType: String? = null,
    val checksum: String? = null,
    val status: String = WordAudioAssetStatus.EMPTY.storageValue,
    val fetchedAt: Instant? = null,
    val lastPlayedAt: Instant? = null,
    val lastError: String? = null,
    val failureCount: Int = 0,
)

