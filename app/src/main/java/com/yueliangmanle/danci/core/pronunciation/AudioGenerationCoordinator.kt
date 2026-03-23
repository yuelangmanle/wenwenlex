package com.yueliangmanle.danci.core.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.AudioGenerationRepository
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildAudioGenerationRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildPronunciationSourceRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.model.AudioGenerationTask
import com.yueliangmanle.danci.core.model.AudioGenerationTaskItem
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourcePreset
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.worker.AudioGenerationScheduler
import java.time.Instant
import java.util.UUID

const val AUDIO_GENERATION_SCOPE_WORD = "word"
const val AUDIO_GENERATION_SCOPE_BOOK = "book"
const val AUDIO_GENERATION_SCOPE_BATCH = "batch"
const val AUDIO_GENERATION_TASK_STATUS_QUEUED = "queued"
const val AUDIO_GENERATION_TASK_STATUS_RUNNING = "running"
const val AUDIO_GENERATION_TASK_STATUS_COMPLETED = "completed"
const val AUDIO_GENERATION_TASK_STATUS_FAILED = "failed"
const val AUDIO_GENERATION_ITEM_STATUS_QUEUED = "queued"
const val AUDIO_GENERATION_ITEM_STATUS_RUNNING = "running"
const val AUDIO_GENERATION_ITEM_STATUS_COMPLETED = "completed"
const val AUDIO_GENERATION_ITEM_STATUS_FAILED = "failed"
const val DEFAULT_AUDIO_GENERATION_BATCH_SIZE = 50

interface AudioGenerationWorkScheduler {
    suspend fun enqueue(
        taskId: String,
        batchSize: Int = DEFAULT_AUDIO_GENERATION_BATCH_SIZE,
        requiresNetwork: Boolean,
    )
}

class AudioGenerationCoordinator(
    private val audioGenerationRepository: AudioGenerationRepository,
    private val pronunciationSourceRepository: PronunciationSourceRepository,
    private val wordRepository: WordRepository,
    private val bookRepository: BookRepository,
    private val scheduler: AudioGenerationWorkScheduler,
    private val resolver: SourcePlaybackResolver = SourcePlaybackResolver(),
    private val idGenerator: () -> String = { "audio-gen-${UUID.randomUUID()}" },
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    suspend fun enqueueSingleWord(
        sourceId: String,
        wordId: Long,
        presetId: String? = null,
        runInBackground: Boolean = true,
    ): String {
        val word = requireNotNull(wordRepository.getWord(wordId)) {
            "没有找到要生成缓存的单词。"
        }
        val task = buildTask(
            source = requireSupportedSource(sourceId),
            presetId = presetId,
            scopeType = AUDIO_GENERATION_SCOPE_WORD,
            scopeRef = wordId.toString(),
            words = listOf(word),
        )
        audioGenerationRepository.upsertTasks(listOf(task))
        if (runInBackground) {
            enqueueTask(task, requireSupportedSource(sourceId))
        }
        return task.id
    }

    suspend fun enqueueBook(
        sourceId: String,
        bookId: String,
        presetId: String? = null,
        batchSize: Int = DEFAULT_AUDIO_GENERATION_BATCH_SIZE,
        runInBackground: Boolean = true,
    ): String {
        val source = requireSupportedSource(sourceId)
        val words = bookRepository.getWords(bookId)
        require(words.isNotEmpty()) { "当前词书里还没有可生成缓存的单词。" }
        val task = buildTask(
            source = source,
            presetId = presetId,
            scopeType = AUDIO_GENERATION_SCOPE_BOOK,
            scopeRef = bookId,
            words = words,
        )
        audioGenerationRepository.upsertTasks(listOf(task))
        if (runInBackground) {
            enqueueTask(task, source, batchSize)
        }
        return task.id
    }

    suspend fun enqueueBookBatch(
        sourceId: String,
        bookId: String,
        presetId: String? = null,
        batchSize: Int = DEFAULT_AUDIO_GENERATION_BATCH_SIZE,
        runInBackground: Boolean = true,
    ): String {
        val source = requireSupportedSource(sourceId)
        val resolvedBatchSize = batchSize.coerceAtLeast(1)
        val words = bookRepository.getWords(bookId).take(resolvedBatchSize)
        require(words.isNotEmpty()) { "当前词书里还没有可生成缓存的单词。" }
        val task = buildTask(
            source = source,
            presetId = presetId,
            scopeType = AUDIO_GENERATION_SCOPE_BATCH,
            scopeRef = "$bookId:$resolvedBatchSize",
            words = words,
        )
        audioGenerationRepository.upsertTasks(listOf(task))
        if (runInBackground) {
            enqueueTask(task, source, resolvedBatchSize)
        }
        return task.id
    }

    suspend fun retryFailedItems(taskId: String): Int {
        val existing = requireNotNull(audioGenerationRepository.getTask(taskId)) {
            "没有找到要重试的任务。"
        }
        val retriedCount = existing.items.count { it.status == AUDIO_GENERATION_ITEM_STATUS_FAILED }
        if (retriedCount <= 0) {
            return 0
        }
        val source = requireSupportedSource(existing.sourceId)
        val updatedTask = existing.copy(
            status = AUDIO_GENERATION_TASK_STATUS_QUEUED,
            updatedAt = nowProvider(),
            items = existing.items.map { item ->
                if (item.status == AUDIO_GENERATION_ITEM_STATUS_FAILED) {
                    item.copy(
                        status = AUDIO_GENERATION_ITEM_STATUS_QUEUED,
                        failureReason = null,
                    )
                } else {
                    item
                }
            },
        ).refreshProgress()
        audioGenerationRepository.upsertTasks(listOf(updatedTask))
        enqueueTask(updatedTask, source)
        return retriedCount
    }

    private suspend fun requireSupportedSource(
        sourceId: String,
    ): PronunciationSource {
        val source = requireNotNull(pronunciationSourceRepository.getSource(sourceId)) {
            "没有找到对应发音源。"
        }
        val support = resolver.resolveGenerationSupport(source)
        require(support.supported) { support.failureReason ?: "当前发音源暂不支持后台生成。" }
        return source
    }

    private fun buildTask(
        source: PronunciationSource,
        presetId: String?,
        scopeType: String,
        scopeRef: String,
        words: List<Word>,
    ): AudioGenerationTask {
        val taskId = idGenerator()
        val now = nowProvider()
        return AudioGenerationTask(
            id = taskId,
            sourceId = source.id,
            presetId = resolvePresetId(source, presetId),
            scopeType = scopeType,
            scopeRef = scopeRef,
            status = AUDIO_GENERATION_TASK_STATUS_QUEUED,
            totalItems = words.size,
            completedItems = 0,
            failedItems = 0,
            items = words.mapIndexed { index, word ->
                AudioGenerationTaskItem(
                    taskId = taskId,
                    itemKey = word.id.takeIf { it > 0L }?.toString() ?: "${scopeRef}_$index",
                    wordId = word.id.takeIf { it > 0L },
                    text = word.lemma,
                    status = AUDIO_GENERATION_ITEM_STATUS_QUEUED,
                )
            },
            createdAt = now,
            updatedAt = now,
        ).refreshProgress()
    }

    private suspend fun enqueueTask(
        task: AudioGenerationTask,
        source: PronunciationSource,
        batchSize: Int = DEFAULT_AUDIO_GENERATION_BATCH_SIZE,
    ) {
        scheduler.enqueue(
            taskId = task.id,
            batchSize = batchSize.coerceAtLeast(1),
            requiresNetwork = resolver.requiresNetwork(source),
        )
    }

    private fun resolvePresetId(
        source: PronunciationSource,
        presetId: String?,
    ): String? {
        val explicitPresetId = presetId?.takeIf { candidate ->
            source.presets.any { it.presetId == candidate }
        }
        if (explicitPresetId != null) {
            return explicitPresetId
        }
        return source.presets.firstOrNull(PronunciationSourcePreset::isDefaultPreset)?.presetId
            ?: source.presets.firstOrNull()?.presetId
    }
}

internal fun AudioGenerationTask.refreshProgress(): AudioGenerationTask {
    val completedItems = items.count { it.status == AUDIO_GENERATION_ITEM_STATUS_COMPLETED }
    val failedItems = items.count { it.status == AUDIO_GENERATION_ITEM_STATUS_FAILED }
    val hasRunningItems = items.any { it.status == AUDIO_GENERATION_ITEM_STATUS_RUNNING }
    val hasQueuedItems = items.any { it.status == AUDIO_GENERATION_ITEM_STATUS_QUEUED }
    val resolvedStatus = when {
        hasRunningItems -> AUDIO_GENERATION_TASK_STATUS_RUNNING
        hasQueuedItems -> AUDIO_GENERATION_TASK_STATUS_QUEUED
        failedItems > 0 -> AUDIO_GENERATION_TASK_STATUS_FAILED
        else -> AUDIO_GENERATION_TASK_STATUS_COMPLETED
    }
    return copy(
        status = resolvedStatus,
        totalItems = items.size,
        completedItems = completedItems,
        failedItems = failedItems,
    )
}

fun buildAudioGenerationCoordinator(
    context: Context,
): AudioGenerationCoordinator {
    val appContext = context.applicationContext
    return AudioGenerationCoordinator(
        audioGenerationRepository = buildAudioGenerationRepository(appContext),
        pronunciationSourceRepository = buildPronunciationSourceRepository(appContext),
        wordRepository = buildWordRepository(appContext),
        bookRepository = buildBookRepository(appContext),
        scheduler = AudioGenerationScheduler(appContext),
    )
}
