package com.yueliangmanle.danci.core.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildAiMemoryRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.data.buildWordAudioRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.model.buildCachedDictionaryStatusMessage
import com.yueliangmanle.danci.core.model.buildRemoteDictionaryStatusMessage
import com.yueliangmanle.danci.core.model.PlaybackResult
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationMode
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.model.Word
import java.time.Duration
import java.time.Instant

class PronunciationOrchestrator(
    private val settingsRepository: SettingsRepository,
    private val wordRepository: WordRepository,
    private val wordAudioRepository: WordAudioRepository,
    private val voicePackRepository: VoicePackRepository,
    private val dictionaryAudioService: DictionaryAudioService,
    private val offlineTtsEngine: OfflineTtsEngine,
    private val systemTtsEngine: SystemTtsEngine,
    private val telemetryRecorder: PlaybackTelemetryRecorder,
    private val audioPlayer: suspend (String?) -> Boolean = ::playAudioFile,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private val nativeFailureCooldowns = mutableMapOf<String, NativeFailureCooldown>()

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
        val startedAt = nowProvider()
        val normalizedWord = normalizeWordForPronunciation(word.lemma)
        var nativeFailure: NativeFailureContext? = null

        suspend fun recordPlayback(
            result: PlaybackResult,
            errorMessage: String? = result.errorMessage ?: nativeFailure?.errorMessage,
            failureStageOverride: String? = result.failureStage ?: nativeFailure?.stage,
            fallbackUsedOverride: Boolean = result.fallbackUsed || nativeFailure != null,
        ) {
            telemetryRecorder.recordWordPlayback(
                wordId = word.id,
                lemma = word.lemma,
                normalizedWord = normalizedWord,
                source = result.source,
                accent = result.accent,
                contextLabel = contextLabel,
                success = result.success,
                cacheHit = result.cacheHit,
                latencyMs = Duration.between(startedAt, nowProvider()).toMillis().coerceAtLeast(0L),
                voicePackId = result.voicePackId ?: nativeFailure?.voicePack?.id,
                voicePackVersion = result.voicePackVersion ?: nativeFailure?.voicePack?.version,
                failureStage = failureStageOverride,
                fallbackUsed = fallbackUsedOverride,
                errorMessage = errorMessage,
            )
        }

        suspend fun recordHardFailure(
            errorMessage: String,
        ) {
            telemetryRecorder.recordWordPlayback(
                wordId = word.id,
                lemma = word.lemma,
                normalizedWord = normalizedWord,
                source = PlaybackSource.SYSTEM_TTS,
                accent = accent,
                contextLabel = contextLabel,
                success = false,
                cacheHit = false,
                latencyMs = Duration.between(startedAt, nowProvider()).toMillis().coerceAtLeast(0L),
                voicePackId = nativeFailure?.voicePack?.id,
                voicePackVersion = nativeFailure?.voicePack?.version,
                failureStage = nativeFailure?.stage,
                fallbackUsed = nativeFailure != null,
                errorMessage = errorMessage,
            )
        }

        suspend fun playBridgeOfflineIfAvailable(): PlaybackResult? {
            val activePack = voicePackRepository.getActiveVoicePack(accent) ?: return null
            if (VoicePackEngineType.fromStorageValue(activePack.engineType) != VoicePackEngineType.SYSTEM_TTS_BRIDGE) {
                return null
            }
            val offlineResult = offlineTtsEngine.speakWord(word, accent)
            if (offlineResult != null) {
                recordPlayback(offlineResult)
            }
            return offlineResult
        }

        suspend fun playNativeIfAvailable(): PlaybackResult? {
            val activeNativePack = resolveActiveNativePack(accent) ?: return null
            currentNativeCooldown(activeNativePack, accent)?.let { cooldown ->
                nativeFailure = cooldown.failure
                return null
            }
            return try {
                offlineTtsEngine.speakNativeWordIfAvailable(word, accent)
            } catch (error: Throwable) {
                nativeFailure = NativeFailureContext(
                    voicePack = activeNativePack,
                    stage = classifyNativeFailureStage(error),
                    errorMessage = error.message ?: "原生离线发音失败",
                )
                nativeFailureCooldowns[cooldownKey(activeNativePack, accent)] = NativeFailureCooldown(
                    until = nowProvider().plus(NATIVE_FAILURE_COOLDOWN),
                    failure = requireNotNull(nativeFailure),
                )
                null
            }
        }

        wordAudioRepository.findCachedAsset(word.id, accent)?.let { asset ->
            val resolvedAccent = PronunciationAccent.fromStorageValue(asset.accent)
            if (audioPlayer(asset.localPath)) {
                wordAudioRepository.markPlayed(asset)
                val result = PlaybackResult(
                    success = true,
                    source = PlaybackSource.DICTIONARY_CACHE,
                    accent = resolvedAccent,
                    statusMessage = buildCachedDictionaryStatusMessage(resolvedAccent),
                    cacheHit = true,
                )
                recordPlayback(result, fallbackUsedOverride = false)
                return result
            }
        }

        playNativeIfAvailable()?.let { nativeResult ->
            recordPlayback(nativeResult, fallbackUsedOverride = false)
            return nativeResult
        }

        if (pronunciationMode == PronunciationMode.OFFLINE_FIRST) {
            playBridgeOfflineIfAvailable()?.let { return it }
        }

        if (!wordAudioRepository.isRemoteLookupCoolingDown(word.id, accent)) {
            val candidates = dictionaryAudioService.resolveCandidates(word.lemma, accent)
            if (candidates.isNotEmpty()) {
                for (candidate in candidates) {
                    val cachedAsset = wordAudioRepository.cacheDictionaryAudio(
                        wordId = word.id,
                        candidate = candidate,
                    )
                    if (cachedAsset != null && audioPlayer(cachedAsset.localPath)) {
                        val resolvedAccent = PronunciationAccent.fromStorageValue(cachedAsset.accent)
                        wordAudioRepository.markPlayed(cachedAsset)
                        val result = PlaybackResult(
                            success = true,
                            source = PlaybackSource.DICTIONARY_REMOTE,
                            accent = resolvedAccent,
                            statusMessage = buildRemoteDictionaryStatusMessage(
                                accent = resolvedAccent,
                                sourceLabel = candidate.sourceLabel,
                            ),
                            fallbackUsed = nativeFailure != null,
                            failureStage = nativeFailure?.stage,
                            voicePackId = nativeFailure?.voicePack?.id,
                            voicePackVersion = nativeFailure?.voicePack?.version,
                        )
                        recordPlayback(result)
                        return result
                    }
                }
                wordAudioRepository.markRemoteLookupFailure(
                    wordId = word.id,
                    accent = accent,
                    errorMessage = "在线词典音频候选均下载失败",
                )
            } else {
                wordAudioRepository.markRemoteLookupFailure(
                    wordId = word.id,
                    accent = accent,
                    errorMessage = "没有查到可用词典音频",
                )
            }
        }

        if (pronunciationMode == PronunciationMode.DICTIONARY_FIRST) {
            playBridgeOfflineIfAvailable()?.let { return it }
        }

        if (settings.fallbackToSystemTts && systemTtsEngine.speak(word.lemma, accent)) {
            val result = PlaybackResult(
                success = true,
                source = PlaybackSource.SYSTEM_TTS,
                accent = accent,
                statusMessage = "当前使用系统朗读。",
                fallbackUsed = nativeFailure != null,
                failureStage = nativeFailure?.stage,
                voicePackId = nativeFailure?.voicePack?.id,
                voicePackVersion = nativeFailure?.voicePack?.version,
            )
            recordPlayback(result)
            return result
        }

        val failure = PlaybackResult(
            success = false,
            source = PlaybackSource.SYSTEM_TTS,
            accent = accent,
            errorMessage = "没有找到可用发音资源。",
            fallbackUsed = nativeFailure != null,
            failureStage = nativeFailure?.stage,
            voicePackId = nativeFailure?.voicePack?.id,
            voicePackVersion = nativeFailure?.voicePack?.version,
        )
        recordHardFailure(failure.errorMessage ?: "没有找到可用发音资源")
        return failure
    }

    suspend fun clearDictionaryCache(): String {
        val cleared = wordAudioRepository.clearDictionaryCache()
        return "已清理 $cleared 条缓存音频。"
    }

    suspend fun dictionaryCacheSizeBytes(): Long = wordAudioRepository.cacheSizeBytes()

    private suspend fun resolveActiveNativePack(
        accent: PronunciationAccent,
    ): VoicePack? =
        voicePackRepository.getActiveVoicePack(accent)?.takeIf { pack ->
            pack.status == VoicePackStatus.READY.storageValue &&
                VoicePackEngineType.fromStorageValue(pack.engineType) == VoicePackEngineType.SHERPA_ONNX
        }

    private fun cooldownKey(
        voicePack: VoicePack,
        accent: PronunciationAccent,
    ): String = "${voicePack.id}:${accent.storageValue}"

    private fun currentNativeCooldown(
        voicePack: VoicePack,
        accent: PronunciationAccent,
    ): NativeFailureCooldown? {
        val cooldown = nativeFailureCooldowns[cooldownKey(voicePack, accent)] ?: return null
        return cooldown.takeIf { nowProvider().isBefore(it.until) }
    }
}

fun buildPronunciationOrchestrator(context: Context): PronunciationOrchestrator {
    val appContext = context.applicationContext
    val systemTtsEngine = SystemTtsEngine(appContext)
    val voicePackRepository = buildVoicePackRepository(appContext)
    val wordAudioRepository = buildWordAudioRepository(appContext)
    return PronunciationOrchestrator(
        settingsRepository = buildSettingsRepository(appContext),
        wordRepository = buildWordRepository(appContext),
        wordAudioRepository = wordAudioRepository,
        voicePackRepository = voicePackRepository,
        dictionaryAudioService = DictionaryAudioService(),
        offlineTtsEngine = OfflineTtsEngine(
            voicePackRepository = voicePackRepository,
            bridgeSpeaker = systemTtsEngine,
            nativeWordTtsEngine = NativeOfflineWordTtsEngine(
                context = appContext,
                voicePackRepository = voicePackRepository,
                wordAudioRepository = wordAudioRepository,
            ),
        ),
        systemTtsEngine = systemTtsEngine,
        telemetryRecorder = PlaybackTelemetryRecorder(buildAiMemoryRepository(appContext)),
    )
}

private val NATIVE_FAILURE_COOLDOWN: Duration = Duration.ofMinutes(10)

private data class NativeFailureContext(
    val voicePack: VoicePack,
    val stage: String,
    val errorMessage: String,
)

private data class NativeFailureCooldown(
    val until: Instant,
    val failure: NativeFailureContext,
)

private fun classifyNativeFailureStage(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        "jni runtime" in message.lowercase() -> "runtime_load"
        "runtime artifact" in message.lowercase() -> "runtime_load"
        "empty wav" in message.lowercase() -> "cache_write"
        "save wav" in message.lowercase() -> "cache_write"
        else -> "native_synthesis"
    }
}
