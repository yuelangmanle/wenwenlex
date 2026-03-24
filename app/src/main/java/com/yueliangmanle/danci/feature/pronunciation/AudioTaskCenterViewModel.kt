package com.yueliangmanle.danci.feature.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.AudioGenerationRepository
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildAudioGenerationRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobStatus
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationJobType
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationScope
import com.yueliangmanle.danci.core.worker.AudioGenerationController
import com.yueliangmanle.danci.core.worker.buildAudioGenerationController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioTaskCenterUiState(
    val isLoading: Boolean = false,
    val selectedScopeType: String = AudioGenerationScope.ACTIVE_BOOK.scopeType,
    val scopeOptions: List<AudioTaskScopeOptionUiState> = emptyList(),
    val createOptions: List<AudioTaskCreateOptionUiState> = emptyList(),
    val jobs: List<AudioTaskJobItemUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    companion object {
        fun loading(): AudioTaskCenterUiState = AudioTaskCenterUiState(isLoading = true)
    }
}

data class AudioTaskScopeOptionUiState(
    val scopeType: String,
    val label: String,
    val countLabel: String,
    val isSelected: Boolean,
)

data class AudioTaskCreateOptionUiState(
    val jobType: String,
    val label: String,
    val description: String,
)

data class AudioTaskJobItemUiState(
    val id: Long,
    val label: String,
    val scopeLabel: String,
    val progressLabel: String,
    val statusLabel: String,
    val errorMessage: String?,
    val canPause: Boolean,
    val canResume: Boolean,
    val canCancel: Boolean,
)

class AudioTaskCenterViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: AudioGenerationRepository,
    private val controller: AudioGenerationController,
    private val bookRepository: BookRepository,
    private val wordRepository: WordRepository,
) {
    suspend fun loadUiState(
        selectedScopeType: String = AudioGenerationScope.ACTIVE_BOOK.scopeType,
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): AudioTaskCenterUiState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val activeBook = settings.activeBookId?.let { bookId ->
            bookRepository.getBook(bookId)
        } ?: bookRepository.getAllBooks().firstOrNull()
        val activeBookCount = activeBook?.let { bookRepository.countWords(it.id) } ?: 0
        val allWordCount = wordRepository.getAllWords().size
        AudioTaskCenterUiState(
            selectedScopeType = selectedScopeType,
            scopeOptions = listOf(
                AudioTaskScopeOptionUiState(
                    scopeType = AudioGenerationScope.ACTIVE_BOOK.scopeType,
                    label = activeBook?.title ?: "当前词书",
                    countLabel = "${activeBookCount} 词",
                    isSelected = selectedScopeType == AudioGenerationScope.ACTIVE_BOOK.scopeType,
                ),
                AudioTaskScopeOptionUiState(
                    scopeType = AudioGenerationScope.ALL_WORDS.scopeType,
                    label = "全部单词",
                    countLabel = "${allWordCount} 词",
                    isSelected = selectedScopeType == AudioGenerationScope.ALL_WORDS.scopeType,
                ),
            ),
            createOptions = AudioGenerationJobType.entries.map { type ->
                AudioTaskCreateOptionUiState(
                    jobType = type.storageValue,
                    label = type.label,
                    description = when (type) {
                        AudioGenerationJobType.DICTIONARY_PREFETCH -> "批量预取在线词典音频缓存"
                        AudioGenerationJobType.CLOUD_TTS_PREFETCH -> "批量生成云端 TTS 音频缓存"
                        AudioGenerationJobType.OFFLINE_NATIVE_PREFETCH -> "批量生成本地离线音频缓存"
                    },
                )
            },
            jobs = repository.getAll().map { job ->
                val status = AudioGenerationJobStatus.fromStorageValue(job.status)
                AudioTaskJobItemUiState(
                    id = job.id,
                    label = AudioGenerationJobType.fromStorageValue(job.jobType).label,
                    scopeLabel = when (job.scopeType) {
                        AudioGenerationScope.ACTIVE_BOOK.scopeType -> "当前词书"
                        AudioGenerationScope.ALL_WORDS.scopeType -> "全部单词"
                        else -> job.scopeType
                    },
                    progressLabel = "${job.completedCount}/${job.totalCount}",
                    statusLabel = status.label,
                    errorMessage = job.lastError,
                    canPause = status == AudioGenerationJobStatus.QUEUED || status == AudioGenerationJobStatus.RUNNING,
                    canResume = status == AudioGenerationJobStatus.PAUSED,
                    canCancel = status == AudioGenerationJobStatus.QUEUED ||
                        status == AudioGenerationJobStatus.RUNNING ||
                        status == AudioGenerationJobStatus.PAUSED,
                )
            },
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun createJob(
        jobType: String,
        selectedScopeType: String,
    ): AudioTaskCenterUiState {
        val settings = settingsRepository.getSettings()
        val scope = resolveScope(selectedScopeType, settings.activeBookId)
        val totalCount = resolveTotalCount(scope)
        controller.enqueue(
            scope = scope,
            jobType = jobType,
            totalCount = totalCount,
        )
        return loadUiState(
            selectedScopeType = selectedScopeType,
            statusMessage = "已创建${AudioGenerationJobType.fromStorageValue(jobType).label}任务。",
        )
    }

    suspend fun pauseJob(
        jobId: Long,
        selectedScopeType: String = AudioGenerationScope.ACTIVE_BOOK.scopeType,
    ): AudioTaskCenterUiState {
        controller.pause(jobId)
        return loadUiState(
            selectedScopeType = selectedScopeType,
            statusMessage = "任务已暂停。",
        )
    }

    suspend fun resumeJob(
        jobId: Long,
        selectedScopeType: String = AudioGenerationScope.ACTIVE_BOOK.scopeType,
    ): AudioTaskCenterUiState {
        controller.resume(jobId)
        return loadUiState(
            selectedScopeType = selectedScopeType,
            statusMessage = "任务已继续。",
        )
    }

    suspend fun cancelJob(
        jobId: Long,
        selectedScopeType: String = AudioGenerationScope.ACTIVE_BOOK.scopeType,
    ): AudioTaskCenterUiState {
        controller.cancel(jobId)
        return loadUiState(
            selectedScopeType = selectedScopeType,
            statusMessage = "任务已取消。",
        )
    }

    private suspend fun resolveTotalCount(scope: AudioGenerationScope): Int =
        when (scope.scopeType) {
            AudioGenerationScope.ALL_WORDS.scopeType -> wordRepository.getAllWords().size
            else -> scope.scopeRef.takeIf(String::isNotBlank)?.let { bookId ->
                bookRepository.countWords(bookId)
            } ?: 0
        }

    private fun resolveScope(
        selectedScopeType: String,
        activeBookId: String?,
    ): AudioGenerationScope =
        when (selectedScopeType) {
            AudioGenerationScope.ALL_WORDS.scopeType -> AudioGenerationScope.allWords()
            else -> AudioGenerationScope.activeBook(activeBookId)
        }
}

suspend fun loadAudioTaskCenterViewModel(context: Context): AudioTaskCenterViewModel =
    withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        AudioTaskCenterViewModel(
            settingsRepository = buildSettingsRepository(appContext),
            repository = buildAudioGenerationRepository(appContext),
            controller = buildAudioGenerationController(appContext),
            bookRepository = buildBookRepository(appContext),
            wordRepository = buildWordRepository(appContext),
        )
    }
