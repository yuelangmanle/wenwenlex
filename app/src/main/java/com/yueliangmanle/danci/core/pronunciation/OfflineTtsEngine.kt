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
) {
    suspend fun speakWord(
        word: Word,
        accent: PronunciationAccent,
    ): PlaybackResult? {
        val activePack = voicePackRepository.getActiveVoicePack() ?: return null
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
                    )
                } else {
                    null
                }
            }
            VoicePackEngineType.SHERPA_ONNX -> null
        }
    }
}
