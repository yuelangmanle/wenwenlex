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
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.worker.VoicePackDownloadController
import com.yueliangmanle.danci.core.worker.buildVoicePackDownloadController
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
    val versionLabel: String,
    val engineLabel: String,
    val capabilitySummary: String,
    val resourceHint: String,
    val downloadSourceSummary: String,
    val statusLabel: String,
    val failureReason: String? = null,
    val isActive: Boolean,
    val isBusy: Boolean,
    val canActivate: Boolean,
    val canDownload: Boolean,
    val canCancelDownload: Boolean,
    val canSwitchDownloadSource: Boolean,
    val canDelete: Boolean,
)

class PronunciationSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val wordAudioRepository: WordAudioRepository,
    private val voicePackRepository: VoicePackRepository,
    private val voicePackDownloadController: VoicePackDownloadController,
) {
    suspend fun loadUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): PronunciationSettingsUiState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val cacheBytes = wordAudioRepository.cacheSizeBytes()
        val voicePacks = voicePackRepository.getAllVoicePacks().map { pack ->
            buildVoicePackItemUiState(
                pack = pack,
                failureCode = if (pack.status == VoicePackStatus.BROKEN.storageValue) {
                    voicePackDownloadController.latestFailureCode(pack.id)
                } else {
                    null
                },
                failureReason = if (pack.status == VoicePackStatus.BROKEN.storageValue) {
                    voicePackDownloadController.latestFailureMessage(pack.id)
                } else {
                    null
                },
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

    suspend fun refreshCatalog(): PronunciationSettingsUiState {
        val syncedCount = voicePackRepository.refreshCatalog()
        return loadUiState(statusMessage = "已同步 $syncedCount 个语音包。")
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
        val voicePack = voicePackRepository.getVoicePack(id)
            ?: return loadUiState(errorMessage = "没有找到对应语音包。")
        if (voicePack.status != VoicePackStatus.READY.storageValue) {
            return loadUiState(errorMessage = "语音包尚未安装完成，暂时不能启用。")
        }
        voicePackRepository.activateVoicePack(id)
        settingsRepository.updateActiveVoicePackId(id)
        return loadUiState(statusMessage = "已切换默认语音包。")
    }

    suspend fun downloadVoicePack(id: String): PronunciationSettingsUiState {
        val settings = settingsRepository.getSettings()
        voicePackDownloadController.enqueue(
            voicePackId = id,
            allowCellular = settings.allowCellularVoicePackDownload,
        )
        return loadUiState(statusMessage = "已开始下载并安装语音包。")
    }

    suspend fun cancelVoicePackDownload(id: String): PronunciationSettingsUiState {
        voicePackDownloadController.cancel(id)
        return loadUiState(statusMessage = "已取消语音包下载。")
    }

    suspend fun retryVoicePackDownloadWithMirror(id: String): PronunciationSettingsUiState {
        val voicePack = voicePackRepository.getVoicePack(id)
            ?: return loadUiState(errorMessage = "没有找到对应语音包。")
        val nextMirror = nextDownloadMirror(voicePack)
            ?: return loadUiState(errorMessage = "当前没有可切换的备用下载源。")
        val settings = settingsRepository.getSettings()
        voicePackDownloadController.retryWithMirror(
            voicePackId = id,
            downloadUrl = nextMirror,
            allowCellular = settings.allowCellularVoicePackDownload,
        )
        return loadUiState(statusMessage = "已切换到备用下载源并重新开始下载。")
    }

    suspend fun removeVoicePack(id: String): PronunciationSettingsUiState {
        val activeVoicePackId = settingsRepository.getSettings().activeVoicePackId
        voicePackRepository.removeVoicePack(id)
        if (activeVoicePackId == id) {
            settingsRepository.updateActiveVoicePackId(null)
        }
        return loadUiState(statusMessage = "已移除语音包。")
    }
}

suspend fun loadPronunciationSettingsViewModel(context: Context): PronunciationSettingsViewModel =
    withContext(Dispatchers.IO) {
        PronunciationSettingsViewModel(
            settingsRepository = buildSettingsRepository(context.applicationContext),
            wordAudioRepository = buildWordAudioRepository(context.applicationContext),
            voicePackRepository = buildVoicePackRepository(context.applicationContext),
            voicePackDownloadController = buildVoicePackDownloadController(context.applicationContext),
        )
    }

internal fun buildVoicePackItemUiState(
    pack: VoicePack,
    failureCode: String? = null,
    failureReason: String? = null,
): VoicePackItemUiState {
    val statusLabel = when (pack.status) {
        VoicePackStatus.READY.storageValue -> "已安装"
        VoicePackStatus.DOWNLOADING.storageValue -> "下载中"
        VoicePackStatus.VERIFYING.storageValue -> "校验中"
        VoicePackStatus.INSTALLING.storageValue -> "安装中"
        VoicePackStatus.BROKEN.storageValue -> "安装异常"
        else -> "未安装"
    }
    val isBusy = pack.status == VoicePackStatus.DOWNLOADING.storageValue ||
        pack.status == VoicePackStatus.VERIFYING.storageValue ||
        pack.status == VoicePackStatus.INSTALLING.storageValue
    val isReady = pack.status == VoicePackStatus.READY.storageValue
    val downloadUrls = (listOfNotNull(pack.downloadUrl) + pack.downloadUrls)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
    val currentSourceIndex = pack.downloadUrl
        ?.let(downloadUrls::indexOf)
        ?.takeIf { it >= 0 }
    val engineLabel = when (VoicePackEngineType.fromStorageValue(pack.engineType)) {
        VoicePackEngineType.SYSTEM_TTS_BRIDGE -> "系统语音桥接"
        VoicePackEngineType.SHERPA_ONNX -> "原生离线发音"
    }
    val capabilitySummary = when (VoicePackEngineType.fromStorageValue(pack.engineType)) {
        VoicePackEngineType.SYSTEM_TTS_BRIDGE -> "依赖系统 TTS，不保证覆盖导入词书。"
        VoicePackEngineType.SHERPA_ONNX -> {
            val baseSummary = if (pack.supportsImportedWords) {
                "支持内置词书 + Excel 导入词书发音。"
            } else {
                "当前仅保证内置词书发音。"
            }
            if (pack.modelFamily?.contains("scaffold", ignoreCase = true) == true) {
                "$baseSummary 当前为分发链路版，模型执行桥接仍在接入。"
            } else {
                baseSummary
            }
        }
    }
    val failureCodeLabel = mapVoicePackFailureCode(failureCode)
    val storageHint = pack.estimatedStorageBytes?.let { "%.2f MB".format(it / 1024f / 1024f) }
    val ramHint = pack.estimatedRamMb?.let { "${it} MB RAM" }
    val resourceHint = listOfNotNull(storageHint, ramHint).joinToString(" · ").ifBlank { "资源占用信息待补充" }
    val downloadSourceSummary = when {
        downloadUrls.isEmpty() -> "当前未配置下载源"
        currentSourceIndex == null -> "已配置 ${downloadUrls.size} 个下载源"
        downloadUrls.size == 1 -> "当前下载源 1/1"
        else -> "当前下载源 ${currentSourceIndex + 1}/${downloadUrls.size}"
    }
    val resolvedFailureReasonText = listOfNotNull(failureCodeLabel, failureReason)
        .joinToString("：")
    val resolvedFailureReason = resolvedFailureReasonText.takeIf(String::isNotBlank)
    return VoicePackItemUiState(
        id = pack.id,
        name = pack.name,
        locale = pack.locale,
        versionLabel = "v${pack.version}",
        engineLabel = engineLabel,
        capabilitySummary = capabilitySummary,
        resourceHint = resourceHint,
        downloadSourceSummary = downloadSourceSummary,
        statusLabel = statusLabel,
        failureReason = resolvedFailureReason,
        isActive = pack.isActive,
        isBusy = isBusy,
        canActivate = isReady && !pack.isActive,
        canDownload = !isReady && !isBusy,
        canCancelDownload = pack.status == VoicePackStatus.DOWNLOADING.storageValue,
        canSwitchDownloadSource = downloadUrls.size > 1 &&
            (pack.status == VoicePackStatus.BROKEN.storageValue || pack.status == VoicePackStatus.NOT_INSTALLED.storageValue),
        canDelete = isReady || pack.status == VoicePackStatus.BROKEN.storageValue,
    )
}

private fun mapVoicePackFailureCode(code: String?): String? =
    when (code) {
        "source_timeout" -> "下载超时"
        "source_not_found" -> "下载源不存在"
        "source_forbidden" -> "下载源拒绝访问"
        "source_server_error" -> "下载源服务异常"
        "source_http_error" -> "下载源响应异常"
        "source_io_error" -> "下载过程异常"
        "source_missing" -> "缺少下载地址"
        else -> null
    }

private fun nextDownloadMirror(pack: VoicePack): String? {
    val downloadUrls = (listOfNotNull(pack.downloadUrl) + pack.downloadUrls)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
    if (downloadUrls.size < 2) {
        return null
    }
    val currentIndex = pack.downloadUrl?.let(downloadUrls::indexOf)?.takeIf { it >= 0 } ?: -1
    return if (currentIndex < 0) {
        downloadUrls.firstOrNull()
    } else {
        downloadUrls[(currentIndex + 1) % downloadUrls.size]
    }
}
