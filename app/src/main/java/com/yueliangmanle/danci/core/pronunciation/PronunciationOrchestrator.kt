package com.yueliangmanle.danci.core.pronunciation

import android.content.Context
import android.media.MediaPlayer
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.data.buildWordAudioRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.model.PlaybackResult
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationMode
import com.yueliangmanle.danci.core.model.Word
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PronunciationOrchestrator(
    private val settingsRepository: SettingsRepository,
    private val wordRepository: WordRepository,
    private val wordAudioRepository: WordAudioRepository,
    private val dictionaryAudioService: DictionaryAudioService,
    private val offlineTtsEngine: OfflineTtsEngine,
    private val systemTtsEngine: SystemTtsEngine,
    private val telemetryRecorder: PlaybackTelemetryRecorder,
) {
    suspend fun playWordById(
        wordId: Long,
        accentOverride: PronunciationAccent? = null,
        contextLabel: String = "detail",
    ): PlaybackResult {
        val word = wordRepository.getWord(wordId)
            ?: return PlaybackResult(
                success = false,
                source = PlaybackSource.SYSTEM_TTS,
                accent = accentOverride ?: PronunciationAccent.UK,
                errorMessage = "没有找到对应单词，无法播放发音。",
            )
        return playWord(
            word = word,
            accentOverride = accentOverride,
            contextLabel = contextLabel,
        )
    }

    suspend fun playWord(
        word: Word,
        accentOverride: PronunciationAccent? = null,
        contextLabel: String = "detail",
    ): PlaybackResult {
        val settings = settingsRepository.getSettings()
        val accent = accentOverride ?: PronunciationAccent.fromStorageValue(settings.preferredPronunciationAccent)
        val pronunciationMode = PronunciationMode.fromStorageValue(settings.pronunciationMode)

        suspend fun playOfflineIfAvailable(): PlaybackResult? {
            val offlineResult = offlineTtsEngine.speakWord(word, accent)
            if (offlineResult != null) {
                telemetryRecorder.recordWordPlayback(
                    wordId = word.id,
                    source = PlaybackSource.OFFLINE_TTS,
                    accent = accent,
                    contextLabel = contextLabel,
                    success = offlineResult.success,
                    errorMessage = offlineResult.errorMessage,
                )
            }
            return offlineResult
        }

        wordAudioRepository.findCachedAsset(word.id, accent)?.let { asset ->
            if (playLocalFile(asset.localPath)) {
                wordAudioRepository.markPlayed(asset)
                telemetryRecorder.recordWordPlayback(
                    wordId = word.id,
                    source = PlaybackSource.DICTIONARY_CACHE,
                    accent = accent,
                    contextLabel = contextLabel,
                    success = true,
                )
                return PlaybackResult(
                    success = true,
                    source = PlaybackSource.DICTIONARY_CACHE,
                    accent = accent,
                    statusMessage = "已播放缓存词典音频。",
                )
            }
        }

        if (pronunciationMode == PronunciationMode.OFFLINE_FIRST) {
            playOfflineIfAvailable()?.let { return it }
        }

        if (!wordAudioRepository.isRemoteLookupCoolingDown(word.id, accent)) {
            val candidate = dictionaryAudioService.resolveCandidate(word.lemma, accent)
            if (candidate != null) {
                val cachedAsset = wordAudioRepository.cacheDictionaryAudio(
                    wordId = word.id,
                    candidate = candidate,
                )
                if (cachedAsset != null && playLocalFile(cachedAsset.localPath)) {
                    wordAudioRepository.markPlayed(cachedAsset)
                    telemetryRecorder.recordWordPlayback(
                        wordId = word.id,
                        source = PlaybackSource.DICTIONARY_REMOTE,
                        accent = accent,
                        contextLabel = contextLabel,
                        success = true,
                    )
                    return PlaybackResult(
                        success = true,
                        source = PlaybackSource.DICTIONARY_REMOTE,
                        accent = accent,
                        statusMessage = "已联网获取词典音频。",
                    )
                }
            } else {
                wordAudioRepository.markRemoteLookupFailure(
                    wordId = word.id,
                    accent = accent,
                    errorMessage = "没有查到可用词典音频",
                )
            }
        }

        if (pronunciationMode == PronunciationMode.DICTIONARY_FIRST) {
            playOfflineIfAvailable()?.let { return it }
        }

        if (settings.fallbackToSystemTts && systemTtsEngine.speak(word.lemma, accent)) {
            telemetryRecorder.recordWordPlayback(
                wordId = word.id,
                source = PlaybackSource.SYSTEM_TTS,
                accent = accent,
                contextLabel = contextLabel,
                success = true,
            )
            return PlaybackResult(
                success = true,
                source = PlaybackSource.SYSTEM_TTS,
                accent = accent,
                statusMessage = "当前使用系统朗读。",
            )
        }

        telemetryRecorder.recordWordPlayback(
            wordId = word.id,
            source = PlaybackSource.SYSTEM_TTS,
            accent = accent,
            contextLabel = contextLabel,
            success = false,
            errorMessage = "没有找到可用发音资源",
        )
        return PlaybackResult(
            success = false,
            source = PlaybackSource.SYSTEM_TTS,
            accent = accent,
            errorMessage = "没有找到可用发音资源。",
        )
    }

    suspend fun clearDictionaryCache(): String {
        val cleared = wordAudioRepository.clearDictionaryCache()
        return "已清理 $cleared 条缓存音频。"
    }

    suspend fun dictionaryCacheSizeBytes(): Long = wordAudioRepository.cacheSizeBytes()
}

private object AudioPlaybackController {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun play(localPath: String): Boolean = withContext(Dispatchers.Main) {
        val file = File(localPath)
        if (!file.exists()) {
            return@withContext false
        }
        mediaPlayer?.release()
        val player = MediaPlayer()
        mediaPlayer = player
        runCatching {
            player.setDataSource(localPath)
            player.setOnPreparedListener { prepared ->
                prepared.start()
            }
            player.setOnCompletionListener { completed ->
                completed.release()
                if (mediaPlayer === completed) {
                    mediaPlayer = null
                }
            }
            player.setOnErrorListener { failed, _, _ ->
                failed.release()
                if (mediaPlayer === failed) {
                    mediaPlayer = null
                }
                true
            }
            player.prepareAsync()
        }.isSuccess
    }
}

private suspend fun playLocalFile(localPath: String?): Boolean {
    val path = localPath?.takeIf(String::isNotBlank) ?: return false
    return AudioPlaybackController.play(path)
}

fun buildPronunciationOrchestrator(context: Context): PronunciationOrchestrator {
    val appContext = context.applicationContext
    val systemTtsEngine = SystemTtsEngine(appContext)
    return PronunciationOrchestrator(
        settingsRepository = buildSettingsRepository(appContext),
        wordRepository = buildWordRepository(appContext),
        wordAudioRepository = buildWordAudioRepository(appContext),
        dictionaryAudioService = DictionaryAudioService(),
        offlineTtsEngine = OfflineTtsEngine(
            voicePackRepository = buildVoicePackRepository(appContext),
            bridgeSpeaker = systemTtsEngine,
        ),
        systemTtsEngine = systemTtsEngine,
        telemetryRecorder = PlaybackTelemetryRecorder(buildAiMemoryRepository(appContext)),
    )
}
