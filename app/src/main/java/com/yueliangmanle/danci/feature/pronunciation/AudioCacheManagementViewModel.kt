package com.yueliangmanle.danci.feature.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.AudioCacheBucketSummary
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildWordAudioRepository
import com.yueliangmanle.danci.core.model.PlaybackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioCacheManagementUiState(
    val isLoading: Boolean = false,
    val cacheSummary: String = "缓存为空",
    val cacheLimitMb: Int = 300,
    val cacheBuckets: List<AudioCacheBucketUiState> = emptyList(),
    val canClearAllCaches: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class AudioCacheBucketUiState(
    val sourceType: String,
    val title: String,
    val summary: String,
    val canClear: Boolean,
)

class AudioCacheManagementViewModel(
    private val settingsRepository: SettingsRepository,
    private val wordAudioRepository: WordAudioRepository,
) {
    suspend fun loadUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): AudioCacheManagementUiState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val buckets = wordAudioRepository.summarizeCacheBuckets()
        val cacheBytes = buckets.sumOf(AudioCacheBucketSummary::sizeBytes)
        AudioCacheManagementUiState(
            cacheSummary = if (cacheBytes <= 0L) {
                "缓存为空"
            } else {
                "当前缓存 ${formatCacheSize(cacheBytes)}"
            },
            cacheLimitMb = settings.audioCacheLimitMb,
            cacheBuckets = buckets.map(::buildAudioCacheBucketUiState),
            canClearAllCaches = buckets.isNotEmpty(),
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun clearDictionaryCache(): AudioCacheManagementUiState {
        val cleared = wordAudioRepository.clearDictionaryCache()
        return loadUiState(statusMessage = "已清理 $cleared 条词典音频缓存。")
    }

    suspend fun clearCacheBucket(
        sourceType: String,
    ): AudioCacheManagementUiState {
        val cleared = wordAudioRepository.clearCacheBucket(sourceType)
        val sourceLabel = PlaybackSource.fromStorageValue(sourceType).label
        val readableLabel = if (sourceLabel.contains("缓存")) sourceLabel else "${sourceLabel}缓存"
        return loadUiState(statusMessage = "已清理 $cleared 条$readableLabel。")
    }

    suspend fun clearAllCaches(): AudioCacheManagementUiState {
        val buckets = wordAudioRepository.summarizeCacheBuckets()
        val cleared = buckets.sumOf { bucket ->
            wordAudioRepository.clearCacheBucket(bucket.sourceType)
        }
        return loadUiState(statusMessage = "已清理 $cleared 条本地音频缓存。")
    }
}

suspend fun loadAudioCacheManagementViewModel(context: Context): AudioCacheManagementViewModel =
    withContext(Dispatchers.IO) {
        AudioCacheManagementViewModel(
            settingsRepository = buildSettingsRepository(context.applicationContext),
            wordAudioRepository = buildWordAudioRepository(context.applicationContext),
        )
    }

private fun buildAudioCacheBucketUiState(
    summary: AudioCacheBucketSummary,
): AudioCacheBucketUiState =
    AudioCacheBucketUiState(
        sourceType = summary.sourceType,
        title = summary.title,
        summary = "${summary.itemCount} 条 · ${formatCacheSize(summary.sizeBytes)}",
        canClear = summary.itemCount > 0,
    )

private fun formatCacheSize(sizeBytes: Long): String =
    "%.2f MB".format(sizeBytes / 1024f / 1024f)
