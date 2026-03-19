package com.yueliangmanle.danci.feature.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.data.buildWordAudioRepository
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationMode
import com.yueliangmanle.danci.core.model.VoicePackStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PronunciationSettingsUiState(
    val isLoading: Boolean = false,
    val preferredAccent: String = PronunciationAccent.UK.storageValue,
    val pronunciationMode: String = PronunciationMode.DICTIONARY_FIRST.storageValue,
    val autoCacheWordAudio: Boolean = true,
    val allowCellularVoicePackDownload: Boolean = false,
    val fallbackToSystemTts: Boolean = true,
    val preferOfflineForLongText: Boolean = true,
    val audioCacheLimitMb: Int = 300,
    val audioCacheSummary: String = "缓存为空",
    val voicePacks: List<VoicePackItemUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class VoicePackItemUiState(
    val id: String,
    val name: String,
    val locale: String,
    val statusLabel: String,
    val isActive: Boolean,
)

class PronunciationSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val wordAudioRepository: WordAudioRepository,
    private val voicePackRepository: VoicePackRepository,
) {
    suspend fun loadUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): PronunciationSettingsUiState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val cacheBytes = wordAudioRepository.cacheSizeBytes()
        val voicePacks = voicePackRepository.getAllVoicePacks().map { pack ->
            VoicePackItemUiState(
                id = pack.id,
                name = pack.name,
                locale = pack.locale,
                statusLabel = when (pack.status) {
                    VoicePackStatus.READY.storageValue -> "已安装"
                    VoicePackStatus.DOWNLOADING.storageValue -> "下载中"
                    VoicePackStatus.BROKEN.storageValue -> "异常"
                    else -> "未安装"
                },
                isActive = pack.isActive,
            )
        }
        PronunciationSettingsUiState(
            preferredAccent = settings.preferredPronunciationAccent,
            pronunciationMode = settings.pronunciationMode,
            autoCacheWordAudio = settings.autoCacheWordAudio,
            allowCellularVoicePackDownload = settings.allowCellularVoicePackDownload,
            fallbackToSystemTts = settings.fallbackToSystemTts,
            preferOfflineForLongText = settings.preferOfflineForLongText,
            audioCacheLimitMb = settings.audioCacheLimitMb,
            audioCacheSummary = if (cacheBytes <= 0L) {
                "缓存为空"
            } else {
                "当前缓存 %.2f MB".format(cacheBytes / 1024f / 1024f)
            },
            voicePacks = voicePacks,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun updatePreferredAccent(accent: String): PronunciationSettingsUiState {
        settingsRepository.updatePreferredPronunciationAccent(accent)
        return loadUiState(statusMessage = "默认口音已更新。")
    }

    suspend fun updatePronunciationMode(mode: String): PronunciationSettingsUiState {
        settingsRepository.updatePronunciationMode(mode)
        return loadUiState(statusMessage = "发音来源策略已更新。")
    }

    suspend fun updateAutoCache(enabled: Boolean): PronunciationSettingsUiState {
        settingsRepository.updateAutoCacheWordAudio(enabled)
        return loadUiState(statusMessage = if (enabled) "已开启自动缓存。" else "已关闭自动缓存。")
    }

    suspend fun updateAllowCellular(enabled: Boolean): PronunciationSettingsUiState {
        settingsRepository.updateAllowCellularVoicePackDownload(enabled)
        return loadUiState(statusMessage = if (enabled) "允许移动网络下载语音包。" else "已限制为仅 Wi-Fi 下载语音包。")
    }

    suspend fun updateFallbackToSystemTts(enabled: Boolean): PronunciationSettingsUiState {
        settingsRepository.updateFallbackToSystemTts(enabled)
        return loadUiState(statusMessage = if (enabled) "已启用系统朗读兜底。" else "已关闭系统朗读兜底。")
    }

    suspend fun updatePreferOfflineForLongText(enabled: Boolean): PronunciationSettingsUiState {
        settingsRepository.updatePreferOfflineForLongText(enabled)
        return loadUiState(statusMessage = if (enabled) "长文本会优先尝试离线朗读。" else "长文本不再优先离线朗读。")
    }

    suspend fun clearDictionaryCache(): PronunciationSettingsUiState {
        val cleared = wordAudioRepository.clearDictionaryCache()
        return loadUiState(statusMessage = "已清理 $cleared 条词典音频缓存。")
    }

    suspend fun activateVoicePack(id: String): PronunciationSettingsUiState {
        voicePackRepository.activateVoicePack(id)
        settingsRepository.updateActiveVoicePackId(id)
        return loadUiState(statusMessage = "已切换默认语音包。")
    }
}

suspend fun loadPronunciationSettingsViewModel(context: Context): PronunciationSettingsViewModel =
    withContext(Dispatchers.IO) {
        PronunciationSettingsViewModel(
            settingsRepository = buildSettingsRepository(context.applicationContext),
            wordAudioRepository = buildWordAudioRepository(context.applicationContext),
            voicePackRepository = buildVoicePackRepository(context.applicationContext),
        )
    }
