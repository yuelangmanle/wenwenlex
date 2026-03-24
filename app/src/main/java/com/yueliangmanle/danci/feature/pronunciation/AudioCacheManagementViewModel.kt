package com.yueliangmanle.danci.feature.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.AudioCacheFilter
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildWordAudioRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioCacheManagementUiState(
    val isLoading: Boolean = false,
    val filter: AudioCacheFilter = AudioCacheFilter(),
    val cacheLimitMb: Int = 300,
    val totalBytes: Long = 0L,
    val totalSummary: String = "0 B",
    val overLimitBytes: Long = 0L,
    val overLimitSummary: String? = null,
    val buckets: List<AudioCacheBucketUiState> = emptyList(),
    val books: List<AudioCacheBookOptionUiState> = emptyList(),
    val items: List<AudioCacheItemUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    companion object {
        fun loading(): AudioCacheManagementUiState = AudioCacheManagementUiState(isLoading = true)
    }
}

data class AudioCacheBucketUiState(
    val sourceType: String,
    val label: String,
    val count: Int,
    val sizeSummary: String,
    val isSelected: Boolean,
)

data class AudioCacheBookOptionUiState(
    val id: String,
    val title: String,
    val isSelected: Boolean,
)

data class AudioCacheItemUiState(
    val word: String,
    val meaningsSummary: String,
    val sourceLabel: String,
    val accentLabel: String,
    val sizeSummary: String,
)

class AudioCacheManagementViewModel(
    private val settingsRepository: SettingsRepository,
    private val wordAudioRepository: WordAudioRepository,
    private val bookRepository: BookRepository,
    private val wordRepository: WordRepository,
) {
    suspend fun loadUiState(
        filter: AudioCacheFilter = AudioCacheFilter(),
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): AudioCacheManagementUiState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val buckets = wordAudioRepository.summarizeBySource()
        val queriedEntries = wordAudioRepository.queryAssets(filter)
        val wordsById = wordRepository.getWords(queriedEntries.map { it.wordId }.distinct()).associateBy(Word::id)
        val allowedWordIds = filter.bookId?.let { bookId ->
            bookRepository.getWords(bookId).map(Word::id).toSet()
        }
        val normalizedQuery = filter.query.trim().lowercase()
        val filteredItems = queriedEntries.mapNotNull { entry ->
            val word = wordsById[entry.wordId] ?: return@mapNotNull null
            if (allowedWordIds != null && word.id !in allowedWordIds) {
                return@mapNotNull null
            }
            if (normalizedQuery.isNotEmpty()) {
                val searchText = buildString {
                    append(word.lemma)
                    append(' ')
                    append(word.meanings.joinToString(" "))
                }.lowercase()
                if (!searchText.contains(normalizedQuery)) {
                    return@mapNotNull null
                }
            }
            AudioCacheItemUiState(
                word = word.lemma,
                meaningsSummary = word.meanings.take(2).joinToString(" · ").ifBlank { "暂无释义" },
                sourceLabel = entry.sourceLabel,
                accentLabel = PronunciationAccent.fromStorageValue(entry.accent).label,
                sizeSummary = formatBytes(entry.sizeBytes),
            )
        }
        val totalBytes = buckets.sumOf { it.totalBytes }
        val limitBytes = settings.audioCacheLimitMb * 1024L * 1024L
        val overLimitBytes = (totalBytes - limitBytes).coerceAtLeast(0L)
        AudioCacheManagementUiState(
            filter = filter,
            cacheLimitMb = settings.audioCacheLimitMb,
            totalBytes = totalBytes,
            totalSummary = formatBytes(totalBytes),
            overLimitBytes = overLimitBytes,
            overLimitSummary = overLimitBytes.takeIf { it > 0 }?.let { "已超出 ${formatBytes(it)}" },
            buckets = buckets.map { bucket ->
                AudioCacheBucketUiState(
                    sourceType = bucket.sourceType,
                    label = bucket.sourceLabel,
                    count = bucket.count,
                    sizeSummary = formatBytes(bucket.totalBytes),
                    isSelected = filter.sourceType == bucket.sourceType,
                )
            },
            books = bookRepository.getAllBooks().map { book ->
                AudioCacheBookOptionUiState(
                    id = book.id,
                    title = book.title,
                    isSelected = filter.bookId == book.id,
                )
            },
            items = filteredItems,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun updateCacheLimit(
        limitMb: Int,
        filter: AudioCacheFilter,
    ): AudioCacheManagementUiState {
        settingsRepository.updateAudioCacheLimitMb(limitMb)
        return loadUiState(
            filter = filter,
            statusMessage = "缓存上限已更新为 ${limitMb.coerceIn(50, 2048)} MB。",
        )
    }

    suspend fun clearBucket(
        sourceType: String,
        filter: AudioCacheFilter,
    ): AudioCacheManagementUiState {
        val cleared = wordAudioRepository.clearBucket(sourceType)
        return loadUiState(
            filter = filter.copy(
                sourceType = filter.sourceType.takeUnless { it == sourceType },
            ),
            statusMessage = "已清理 $cleared 条${sourceLabel(sourceType)}。",
        )
    }

    suspend fun trimToConfiguredLimit(filter: AudioCacheFilter): AudioCacheManagementUiState {
        val settings = settingsRepository.getSettings()
        val deleted = wordAudioRepository.evictToLimit(settings.audioCacheLimitMb * 1024L * 1024L)
        return loadUiState(
            filter = filter,
            statusMessage = if (deleted.isEmpty()) {
                "当前缓存没有超过上限。"
            } else {
                "已按上限清理 ${deleted.size} 条音频缓存。"
            },
        )
    }

    private fun sourceLabel(sourceType: String): String =
        com.yueliangmanle.danci.core.model.PlaybackSource.fromStorageValue(sourceType).label
}

private fun formatBytes(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    return when {
        safeBytes >= 1024L * 1024L -> "%.2f MB".format(safeBytes / 1024f / 1024f)
        safeBytes >= 1024L -> "%.2f KB".format(safeBytes / 1024f)
        else -> "${safeBytes} B"
    }
}

suspend fun loadAudioCacheManagementViewModel(context: Context): AudioCacheManagementViewModel =
    withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        AudioCacheManagementViewModel(
            settingsRepository = buildSettingsRepository(appContext),
            wordAudioRepository = buildWordAudioRepository(appContext),
            bookRepository = buildBookRepository(appContext),
            wordRepository = buildWordRepository(appContext),
        )
    }
