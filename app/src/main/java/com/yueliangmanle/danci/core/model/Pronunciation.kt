package com.yueliangmanle.danci.core.model

enum class PronunciationAccent(
    val storageValue: String,
    val label: String,
) {
    UK("uk", "英式"),
    US("us", "美式"),
    AUTO("auto", "自动");

    companion object {
        fun fromStorageValue(value: String?): PronunciationAccent =
            entries.firstOrNull { it.storageValue == value } ?: UK
    }
}

enum class PronunciationMode(
    val storageValue: String,
) {
    DICTIONARY_FIRST("dictionary_first"),
    OFFLINE_FIRST("offline_first");

    companion object {
        fun fromStorageValue(value: String?): PronunciationMode =
            entries.firstOrNull { it.storageValue == value } ?: DICTIONARY_FIRST
    }
}

enum class PlaybackSource(
    val storageValue: String,
    val label: String,
) {
    DICTIONARY_CACHE("dictionary_cache", "缓存词典音频"),
    DICTIONARY_REMOTE("dictionary_remote", "在线词典音频"),
    OFFLINE_TTS("offline_tts", "离线语音包朗读"),
    SYSTEM_TTS("system_tts", "系统朗读");

    companion object {
        fun fromStorageValue(value: String?): PlaybackSource =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM_TTS
    }
}

enum class WordAudioAssetStatus(
    val storageValue: String,
) {
    EMPTY("empty"),
    READY("ready"),
    FAILED("failed"),
    STALE("stale");

    companion object {
        fun fromStorageValue(value: String?): WordAudioAssetStatus =
            entries.firstOrNull { it.storageValue == value } ?: EMPTY
    }
}

enum class VoicePackStatus(
    val storageValue: String,
) {
    NOT_INSTALLED("not_installed"),
    DOWNLOADING("downloading"),
    VERIFYING("verifying"),
    INSTALLING("installing"),
    READY("ready"),
    BROKEN("broken");

    companion object {
        fun fromStorageValue(value: String?): VoicePackStatus =
            entries.firstOrNull { it.storageValue == value } ?: NOT_INSTALLED
    }
}

enum class VoicePackEngineType(
    val storageValue: String,
) {
    SHERPA_ONNX("sherpa_onnx");

    companion object {
        fun fromStorageValue(value: String?): VoicePackEngineType =
            entries.firstOrNull { it.storageValue == value } ?: SHERPA_ONNX
    }
}

data class DictionaryAudioCandidate(
    val url: String,
    val accent: PronunciationAccent,
    val mimeType: String? = null,
)

data class PlaybackResult(
    val success: Boolean,
    val source: PlaybackSource,
    val accent: PronunciationAccent,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

const val DEFAULT_PRONUNCIATION_ACCENT = "uk"
const val DEFAULT_PRONUNCIATION_MODE = "dictionary_first"
const val DEFAULT_AUDIO_CACHE_LIMIT_MB = 300

