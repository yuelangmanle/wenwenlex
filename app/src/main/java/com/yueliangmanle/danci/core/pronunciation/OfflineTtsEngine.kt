package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.model.PlaybackResult
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.model.Word

class OfflineTtsEngine(
    private val voicePackRepository: VoicePackRepository,
) {
    suspend fun speakWord(
        word: Word,
        accent: PronunciationAccent,
    ): PlaybackResult? {
        val activePack = voicePackRepository.getActiveVoicePack() ?: return null
        if (activePack.status != VoicePackStatus.READY.storageValue) {
            return null
        }
        if (activePack.installDir.isNullOrBlank()) {
            return null
        }
        return null
    }
}

