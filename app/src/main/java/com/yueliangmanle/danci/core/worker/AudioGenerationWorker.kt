package com.yueliangmanle.danci.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildAiProfileRepository
import com.yueliangmanle.danci.core.data.buildAudioGenerationRepository
import com.yueliangmanle.danci.core.data.buildPronunciationSourceRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.data.buildWordAudioRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.data.buildGeneratedContentHash
import com.yueliangmanle.danci.core.data.buildGeneratedNamespace
import com.yueliangmanle.danci.core.model.AudioGenerationTask
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourcePreset
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.pronunciation.AUDIO_GENERATION_ITEM_STATUS_COMPLETED
import com.yueliangmanle.danci.core.pronunciation.AUDIO_GENERATION_ITEM_STATUS_FAILED
import com.yueliangmanle.danci.core.pronunciation.AUDIO_GENERATION_ITEM_STATUS_RUNNING
import com.yueliangmanle.danci.core.pronunciation.AUDIO_GENERATION_ITEM_STATUS_QUEUED
import com.yueliangmanle.danci.core.pronunciation.AUDIO_GENERATION_TASK_STATUS_FAILED
import com.yueliangmanle.danci.core.pronunciation.GenerationSupport
import com.yueliangmanle.danci.core.pronunciation.MiMoTtsProvider
import com.yueliangmanle.danci.core.pronunciation.NativeOfflineWordTtsEngine
import com.yueliangmanle.danci.core.pronunciation.SourcePlaybackResolver
import com.yueliangmanle.danci.core.pronunciation.normalizeWordForPronunciation
import com.yueliangmanle.danci.core.pronunciation.refreshProgress
import com.yueliangmanle.danci.core.security.buildAiCredentialStore
import java.io.File

class AudioGenerationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getString(INPUT_TASK_ID)?.takeIf(String::isNotBlank)
            ?: return Result.failure()
        val batchSize = inputData.getInt(INPUT_BATCH_SIZE, 1).coerceAtLeast(1)
        val appContext = applicationContext
        val audioGenerationRepository = buildAudioGenerationRepository(appContext)
        val sourceRepository = buildPronunciationSourceRepository(appContext)
        val wordRepository = buildWordRepository(appContext)
        val wordAudioRepository = buildWordAudioRepository(appContext)
        val voicePackRepository = buildVoicePackRepository(appContext)
        val aiProfileRepository = buildAiProfileRepository(appContext)
        val credentialStore = buildAiCredentialStore(appContext)
        val resolver = SourcePlaybackResolver()
        val task = audioGenerationRepository.getTask(taskId) ?: return Result.failure()
        val source = sourceRepository.getSource(task.sourceId) ?: return Result.failure()
        val support = resolver.resolveGenerationSupport(source)
        if (!support.supported) {
            val failedTask = markAllQueuedItemsFailed(
                task = task,
                reason = support.failureReason ?: "当前发音源不支持后台生成。",
            )
            audioGenerationRepository.upsertTasks(listOf(failedTask))
            return Result.success()
        }

        val nativeEngine = NativeOfflineWordTtsEngine(
            context = appContext,
            voicePackRepository = voicePackRepository,
            wordAudioRepository = wordAudioRepository,
        )
        val miMoProvider = MiMoTtsProvider()
        val updatedItems = task.items.toMutableList()
        var processedAny = false
        var processedCount = 0

        updatedItems.forEachIndexed { index, item ->
            if (processedCount >= batchSize || item.status != AUDIO_GENERATION_ITEM_STATUS_QUEUED) {
                return@forEachIndexed
            }
            processedAny = true
            processedCount += 1
            updatedItems[index] = item.copy(status = AUDIO_GENERATION_ITEM_STATUS_RUNNING)
            updatedItems[index] = runCatching {
                generateItem(
                    task = task,
                    source = source,
                    support = support,
                    item = updatedItems[index],
                    wordRepository = wordRepository,
                    wordAudioRepository = wordAudioRepository,
                    nativeEngine = nativeEngine,
                    miMoProvider = miMoProvider,
                    aiProfileRepository = aiProfileRepository,
                    credentialStore = credentialStore,
                )
            }.getOrElse { error ->
                updatedItems[index].copy(
                    status = AUDIO_GENERATION_ITEM_STATUS_FAILED,
                    failureReason = error.message ?: "生成缓存失败。",
                    attemptCount = updatedItems[index].attemptCount + 1,
                )
            }
        }

        val updatedTask = task.copy(items = updatedItems).refreshProgress()
        audioGenerationRepository.upsertTasks(listOf(updatedTask))

        if (!processedAny) {
            return Result.success()
        }

        if (updatedTask.items.any { it.status == AUDIO_GENERATION_ITEM_STATUS_QUEUED }) {
            AudioGenerationScheduler(appContext).enqueue(
                taskId = updatedTask.id,
                batchSize = batchSize,
                requiresNetwork = resolver.requiresNetwork(source),
            )
        }
        return Result.success()
    }
}

private suspend fun generateItem(
    task: AudioGenerationTask,
    source: PronunciationSource,
    support: GenerationSupport,
    item: com.yueliangmanle.danci.core.model.AudioGenerationTaskItem,
    wordRepository: WordRepository,
    wordAudioRepository: WordAudioRepository,
    nativeEngine: NativeOfflineWordTtsEngine,
    miMoProvider: MiMoTtsProvider,
    aiProfileRepository: com.yueliangmanle.danci.core.data.AiProfileRepository,
    credentialStore: com.yueliangmanle.danci.core.security.AiCredentialStore,
): com.yueliangmanle.danci.core.model.AudioGenerationTaskItem {
    val word = if (item.wordId != null) {
        wordRepository.getWord(item.wordId)
    } else {
        null
    } ?: Word(
        id = item.wordId ?: 0L,
        lemma = item.text,
    )
    val normalizedWord = normalizeWordForPronunciation(word.lemma)
        ?: error("仅支持英文字母、连字符和撇号组成的词条。")
    val accent = PronunciationAccent.fromStorageValue(source.accent)
    return when (PronunciationSourceType.fromStorageValue(source.sourceType)) {
        PronunciationSourceType.LOCAL_NATIVE -> {
            val result = nativeEngine.synthesizeWordForSource(
                word = word,
                sourceId = source.backingVoicePackId ?: source.id,
                accent = accent,
            ) ?: error("本地原生发音源当前不可用。")
            item.copy(
                status = AUDIO_GENERATION_ITEM_STATUS_COMPLETED,
                failureReason = null,
                attemptCount = item.attemptCount + 1,
                generatedAssetId = result.asset.id,
            )
        }
        PronunciationSourceType.CLOUD_TTS -> {
            val profileId = source.providerProfileId ?: error("当前云端发音源还没有绑定 AI 档案。")
            val profile = aiProfileRepository.getProfile(profileId) ?: error("绑定的 AI 档案不存在。")
            val apiKey = credentialStore.readApiKey(profileId) ?: error("当前云端档案还没有保存 API Key。")
            val preset = resolvePreset(source, task.presetId)
            val voice = preset?.voice?.takeIf(String::isNotBlank) ?: "default_en"
            val style = preset?.styleTemplate?.takeIf(String::isNotBlank)
            val ttsResult = miMoProvider.synthesize(
                com.yueliangmanle.danci.core.pronunciation.CloudTtsRequest(
                    profile = profile,
                    apiKey = apiKey,
                    text = normalizedWord,
                    voice = voice,
                    style = style,
                ),
            )
            val tempFile = File.createTempFile("audio-generation-", ".wav")
            tempFile.writeBytes(ttsResult.audioBytes)
            val asset = wordAudioRepository.cacheGeneratedAudioWithContext(
                wordId = word.id,
                accent = accent,
                normalizedWord = normalizedWord,
                playbackSource = support.playbackSource ?: PlaybackSource.ONLINE_PREBUILT_CACHE,
                actualSourceType = support.actualSourceType,
                modelFamily = profile.model.ifBlank { "cloud-tts" },
                versionTag = voice,
                sourceId = source.id,
                presetId = preset?.presetId,
                sourceFile = tempFile,
                namespace = buildGeneratedNamespace(
                    accent = accent,
                    modelFamily = profile.model.ifBlank { "cloud-tts" },
                    packVersion = voice,
                    sourceId = source.id,
                    presetId = preset?.presetId,
                    contentHash = buildGeneratedContentHash(normalizedWord),
                ),
                taskId = task.id,
                mimeType = ttsResult.mimeType,
            ) ?: error("云端音频缓存落盘失败。")
            tempFile.delete()
            item.copy(
                status = AUDIO_GENERATION_ITEM_STATUS_COMPLETED,
                failureReason = null,
                attemptCount = item.attemptCount + 1,
                generatedAssetId = asset.id,
            )
        }
        PronunciationSourceType.LOCAL_BRIDGE,
        PronunciationSourceType.DICTIONARY,
        null -> item.copy(
            status = AUDIO_GENERATION_ITEM_STATUS_FAILED,
            failureReason = support.failureReason ?: "当前发音源暂不支持后台生成。",
            attemptCount = item.attemptCount + 1,
        )
    }
}

private fun resolvePreset(
    source: PronunciationSource,
    presetId: String?,
): PronunciationSourcePreset? =
    source.presets.firstOrNull { it.presetId == presetId }
        ?: source.presets.firstOrNull(PronunciationSourcePreset::isDefaultPreset)
        ?: source.presets.firstOrNull()

private fun markAllQueuedItemsFailed(
    task: AudioGenerationTask,
    reason: String,
): AudioGenerationTask =
    task.copy(
        status = AUDIO_GENERATION_TASK_STATUS_FAILED,
        items = task.items.map { item ->
            if (item.status == AUDIO_GENERATION_ITEM_STATUS_COMPLETED) {
                item
            } else {
                item.copy(
                    status = AUDIO_GENERATION_ITEM_STATUS_FAILED,
                    failureReason = reason,
                    attemptCount = item.attemptCount + 1,
                )
            }
        },
    ).refreshProgress()
