package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.model.PlaybackResult
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.model.Word
import java.io.File

class OfflineTtsEngine(
    private val voicePackRepository: VoicePackRepository,
    private val bridgeSpeaker: SystemTtsEngine,
    private val nativeWordTtsEngine: NativeOfflineWordTtsEngine? = null,
) {
    suspend fun speakNativeWordIfAvailable(
        word: Word,
        accent: PronunciationAccent,
    ): PlaybackResult? {
        val activePack = voicePackRepository.getActiveVoicePack(accent) ?: return null
        if (activePack.status != VoicePackStatus.READY.storageValue) {
            return null
        }
        if (VoicePackEngineType.fromStorageValue(activePack.engineType) != VoicePackEngineType.SHERPA_ONNX) {
            return null
        }

        val result = nativeWordTtsEngine?.synthesizeWord(word, accent) ?: return null
        val resolvedAccent = PronunciationAccent.fromStorageValue(result.asset.accent)
        if (!playAudioFile(result.outputFile.absolutePath)) {
            return null
        }
        nativeWordTtsEngine.markPlayed(result.asset)
        return PlaybackResult(
            success = true,
            source = PlaybackSource.OFFLINE_NATIVE_GENERATED,
            accent = resolvedAccent,
            statusMessage = if (result.cacheHit) {
                "已播放本地离线生成音频。"
            } else {
                "已通过本地离线发音播放。"
            },
            cacheHit = result.cacheHit,
            voicePackId = result.voicePack.id,
            voicePackVersion = result.voicePack.version,
        )
    }

    suspend fun speakWord(
        word: Word,
        accent: PronunciationAccent,
    ): PlaybackResult? {
        val activePack = voicePackRepository.getActiveVoicePack(accent) ?: return null
        if (activePack.status != VoicePackStatus.READY.storageValue) {
            return null
        }
        val installDir = activePack.installDir?.takeIf(String::isNotBlank) ?: return null
        if (!File(installDir).exists()) {
            return null
        }
        return when (VoicePackEngineType.fromStorageValue(activePack.engineType)) {
            VoicePackEngineType.SYSTEM_TTS_BRIDGE -> {
                val resolvedAccent = when {
                    accent != PronunciationAccent.AUTO -> accent
                    activePack.accent.isNotBlank() -> PronunciationAccent.fromStorageValue(activePack.accent)
                    else -> PronunciationAccent.UK
                }
                if (bridgeSpeaker.speak(word.lemma, resolvedAccent)) {
                    PlaybackResult(
                        success = true,
                        source = PlaybackSource.OFFLINE_TTS,
                        accent = resolvedAccent,
                        statusMessage = "已通过已下载语音包播放。",
                        voicePackId = activePack.id,
                        voicePackVersion = activePack.version,
                    )
                } else {
                    null
                }
            }
            VoicePackEngineType.SHERPA_ONNX -> {
                speakNativeWordIfAvailable(word, accent)
            }
        }
    }
}
