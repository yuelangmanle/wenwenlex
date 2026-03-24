package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.PlaybackSource

data class AudioGenerationScope(
    val scopeType: String,
    val scopeRef: String,
) {
    companion object {
        val ACTIVE_BOOK = AudioGenerationScope(
            scopeType = "active_book",
            scopeRef = "active_book",
        )
        val ALL_WORDS = AudioGenerationScope(
            scopeType = "all_words",
            scopeRef = "all_words",
        )

        fun activeBook(bookId: String?): AudioGenerationScope =
            AudioGenerationScope(
                scopeType = ACTIVE_BOOK.scopeType,
                scopeRef = bookId.orEmpty(),
            )

        fun allWords(): AudioGenerationScope = ALL_WORDS
    }
}

enum class AudioGenerationJobType(
    val storageValue: String,
    val label: String,
    val sourceType: String,
) {
    DICTIONARY_PREFETCH(
        storageValue = "dictionary_prefetch",
        label = "在线词典缓存",
        sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
    ),
    CLOUD_TTS_PREFETCH(
        storageValue = "cloud_tts_prefetch",
        label = "云端 TTS 缓存",
        sourceType = PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue,
    ),
    OFFLINE_NATIVE_PREFETCH(
        storageValue = "offline_native_prefetch",
        label = "本地离线生成",
        sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
    );

    companion object {
        fun fromStorageValue(value: String?): AudioGenerationJobType =
            entries.firstOrNull { it.storageValue == value } ?: DICTIONARY_PREFETCH
    }
}

enum class AudioGenerationJobStatus(
    val storageValue: String,
    val label: String,
) {
    QUEUED("queued", "排队中"),
    RUNNING("running", "进行中"),
    PAUSED("paused", "已暂停"),
    COMPLETED("completed", "已完成"),
    CANCELLED("cancelled", "已取消"),
    FAILED("failed", "已失败");

    companion object {
        fun fromStorageValue(value: String?): AudioGenerationJobStatus =
            entries.firstOrNull { it.storageValue == value } ?: QUEUED
    }
}
