package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType

data class PreparedCacheLookup(
    val playbackSource: PlaybackSource,
    val actualSourceType: String,
)

data class GenerationSupport(
    val supported: Boolean,
    val playbackSource: PlaybackSource? = null,
    val actualSourceType: String? = null,
    val failureReason: String? = null,
)

class SourcePlaybackResolver {
    fun resolvePreparedCacheLookup(
        source: PronunciationSource,
    ): PreparedCacheLookup? =
        when (PronunciationSourceType.fromStorageValue(source.sourceType)) {
            PronunciationSourceType.LOCAL_NATIVE -> PreparedCacheLookup(
                playbackSource = PlaybackSource.OFFLINE_NATIVE_GENERATED,
                actualSourceType = PronunciationSourceType.LOCAL_NATIVE.storageValue,
            )
            PronunciationSourceType.CLOUD_TTS -> PreparedCacheLookup(
                playbackSource = PlaybackSource.ONLINE_PREBUILT_CACHE,
                actualSourceType = PronunciationSourceType.CLOUD_TTS.storageValue,
            )
            PronunciationSourceType.DICTIONARY,
            PronunciationSourceType.LOCAL_BRIDGE,
            null -> null
        }

    fun resolveGenerationSupport(
        source: PronunciationSource,
    ): GenerationSupport =
        resolvePreparedCacheLookup(source)?.let { lookup ->
            GenerationSupport(
                supported = true,
                playbackSource = lookup.playbackSource,
                actualSourceType = lookup.actualSourceType,
            )
        } ?: GenerationSupport(
            supported = false,
            failureReason = when (PronunciationSourceType.fromStorageValue(source.sourceType)) {
                PronunciationSourceType.LOCAL_BRIDGE -> "当前桥接语音包暂不支持后台预生成缓存。"
                PronunciationSourceType.DICTIONARY -> "词典来源暂不支持后台预生成缓存。"
                null -> "当前发音源类型暂时无法识别。"
                else -> "当前发音源暂不支持后台预生成缓存。"
            },
        )

    fun requiresNetwork(
        source: PronunciationSource,
    ): Boolean = PronunciationSourceType.fromStorageValue(source.sourceType) == PronunciationSourceType.CLOUD_TTS
}
