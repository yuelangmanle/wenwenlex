package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.AudioCacheBucketSummary
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationSessionPreference
import com.yueliangmanle.danci.core.model.WordAudioAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCacheManagementViewModelTest {
    @Test
    fun loadUiState_showsCacheSummaryAndLimit() = runTest {
        val viewModel = AudioCacheManagementViewModel(
            settingsRepository = CacheSettingsRepository(
                AppSettings(audioCacheLimitMb = 512),
            ),
            wordAudioRepository = CacheWordAudioRepository(
                cacheSizeBytes = 8L * 1024L * 1024L,
            ),
        )

        val state = viewModel.loadUiState()

        assertEquals(512, state.cacheLimitMb)
        assertTrue(state.cacheSummary.contains("8.00 MB"))
        assertEquals(2, state.cacheBuckets.size)
        assertEquals("缓存词典音频", state.cacheBuckets.first().title)
    }

    @Test
    fun clearDictionaryCache_returnsStatusMessage() = runTest {
        val repository = CacheWordAudioRepository(cacheSizeBytes = 2L * 1024L * 1024L)
        repository.clearedCount = 6
        val viewModel = AudioCacheManagementViewModel(
            settingsRepository = CacheSettingsRepository(AppSettings()),
            wordAudioRepository = repository,
        )

        val state = viewModel.clearDictionaryCache()

        assertEquals("已清理 6 条词典音频缓存。", state.statusMessage)
    }

    @Test
    fun clearCacheBucket_clearsGeneratedCacheAndRefreshesSummary() = runTest {
        val repository = CacheWordAudioRepository(
            cacheSizeBytes = 12L * 1024L * 1024L,
        ).apply {
            bucketSummaries = mutableListOf(
                AudioCacheBucketSummary(
                    sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
                    title = PlaybackSource.OFFLINE_NATIVE_GENERATED.label,
                    itemCount = 4,
                    sizeBytes = 12L * 1024L * 1024L,
                ),
            )
            clearedByBucket[PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue] = 4
        }
        val viewModel = AudioCacheManagementViewModel(
            settingsRepository = CacheSettingsRepository(AppSettings()),
            wordAudioRepository = repository,
        )

        val state = viewModel.clearCacheBucket(PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue)

        assertEquals("已清理 4 条离线原生生成音频缓存。", state.statusMessage)
        assertTrue(state.cacheSummary.contains("缓存为空"))
    }
}

private class CacheSettingsRepository(
    initial: AppSettings,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    private var sessionPreference = PronunciationSessionPreference()

    override val settings: Flow<AppSettings> = state

    override suspend fun getSettings(): AppSettings = state.value

    override suspend fun getPronunciationSessionPreference(): PronunciationSessionPreference = sessionPreference

    override suspend fun updateSessionWordPronunciationSourceId(sourceId: String?) {
        sessionPreference = sessionPreference.copy(sessionWordPronunciationSourceId = sourceId)
    }

    override suspend fun updateSessionLongTextPronunciationSourceId(sourceId: String?) {
        sessionPreference = sessionPreference.copy(sessionLongTextPronunciationSourceId = sourceId)
    }

    override suspend fun updateDailyGoal(dailyGoal: Int) = Unit
    override suspend fun updateWeeklyGoal(weeklyGoal: Int) = Unit
    override suspend fun updatePhaseName(phaseName: String?) = Unit
    override suspend fun updatePhaseTargetWords(phaseTargetWords: Int) = Unit
    override suspend fun updateActiveBookId(bookId: String?) = Unit
    override suspend fun updateAiEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiBaseUrl(baseUrl: String) = Unit
    override suspend fun updateAiModel(model: String) = Unit
    override suspend fun updateDefaultAiProfileId(profileId: String?) = Unit
    override suspend fun updateWordHelpProfileId(profileId: String?) = Unit
    override suspend fun updatePlanAdjustmentProfileId(profileId: String?) = Unit
    override suspend fun updatePhoneticFillProfileId(profileId: String?) = Unit
    override suspend fun updateAiPlanAdjustmentEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiSessionCheckpointEnabled(enabled: Boolean) = Unit
    override suspend fun updatePreferredPronunciationAccent(accent: String) = Unit
    override suspend fun updatePronunciationMode(mode: String) = Unit
    override suspend fun updateAllowCellularVoicePackDownload(enabled: Boolean) = Unit
    override suspend fun updateAutoCacheWordAudio(enabled: Boolean) = Unit
    override suspend fun updateAudioCacheLimitMb(limitMb: Int) = Unit
    override suspend fun updateActiveVoicePackId(voicePackId: String?) = Unit
    override suspend fun updateFallbackToSystemTts(enabled: Boolean) = Unit
    override suspend fun updatePreferOfflineForLongText(enabled: Boolean) = Unit
    override suspend fun updateReminderEnabled(enabled: Boolean) = Unit
    override suspend fun updateReminderTime(hour: Int, minute: Int) = Unit
}

private class CacheWordAudioRepository(
    private val cacheSizeBytes: Long = 0L,
) : WordAudioRepository {
    var clearedCount: Int = 0
    var bucketSummaries: MutableList<AudioCacheBucketSummary> = mutableListOf(
        AudioCacheBucketSummary(
            sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
            title = PlaybackSource.DICTIONARY_CACHE.label,
            itemCount = if (cacheSizeBytes > 0L) 6 else 0,
            sizeBytes = cacheSizeBytes,
        ),
        AudioCacheBucketSummary(
            sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
            title = PlaybackSource.OFFLINE_NATIVE_GENERATED.label,
            itemCount = 0,
            sizeBytes = 0L,
        ),
    )
    val clearedByBucket = mutableMapOf<String, Int>()

    override suspend fun findCachedAsset(wordId: Long, accent: PronunciationAccent): WordAudioAsset? = null

    override suspend fun isRemoteLookupCoolingDown(wordId: Long, accent: PronunciationAccent): Boolean = false

    override suspend fun cacheDictionaryAudio(wordId: Long, candidate: DictionaryAudioCandidate): WordAudioAsset? = null

    override suspend fun markRemoteLookupFailure(wordId: Long, accent: PronunciationAccent, errorMessage: String) = Unit

    override suspend fun markPlayed(asset: WordAudioAsset) = Unit

    override suspend fun clearDictionaryCache(): Int = clearedCount

    override suspend fun clearCacheBucket(sourceType: String): Int {
        val cleared = clearedByBucket[sourceType] ?: 0
        bucketSummaries = bucketSummaries
            .map { summary ->
                if (summary.sourceType == sourceType) {
                    summary.copy(itemCount = 0, sizeBytes = 0L)
                } else {
                    summary
                }
            }
            .toMutableList()
        return cleared
    }

    override suspend fun summarizeCacheBuckets(): List<AudioCacheBucketSummary> = bucketSummaries.toList()

    override suspend fun cacheSizeBytes(): Long = bucketSummaries.sumOf(AudioCacheBucketSummary::sizeBytes)
}
