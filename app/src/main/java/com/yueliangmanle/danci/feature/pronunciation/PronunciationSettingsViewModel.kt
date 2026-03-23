package com.yueliangmanle.danci.feature.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.buildPronunciationSourceRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.data.buildWordAudioRepository
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationMode
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.pronunciation.PronunciationSourceRegistry
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
    val defaultWordSourceLabel: String = "未设置",
    val defaultLongTextSourceLabel: String = "未设置",
    val sourceItems: List<PronunciationSourceItemUiState> = emptyList(),
    val voicePacks: List<VoicePackItemUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class PronunciationSourceItemUiState(
    val id: String,
    val title: String,
    val subtitle: String,
    val typeLabel: String,
    val availablePresetLabels: List<String>,
    val isDefaultForWord: Boolean,
    val isDefaultForLongText: Boolean,
    val canSetDefaultForWord: Boolean,
    val canSetDefaultForLongText: Boolean,
)

data class VoicePackItemUiState(
    val id: String,
    val name: String,
    val locale: String,
    val versionLabel: String,
    val engineLabel: String,
    val capabilitySummary: String,
    val resourceHint: String,
    val statusLabel: String,
    val failureReason: String? = null,
    val isActive: Boolean,
    val isBusy: Boolean,
    val canActivate: Boolean,
    val canDownload: Boolean,
    val canDelete: Boolean,
)

class PronunciationSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val wordAudioRepository: WordAudioRepository,
    private val voicePackRepository: VoicePackRepository,
    private val voicePackDownloadController: VoicePackDownloadController,
    private val pronunciationSourceRepository: PronunciationSourceRepository,
    private val pronunciationSourceRegistry: PronunciationSourceRegistry? = null,
) {
    suspend fun loadUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): PronunciationSettingsUiState = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val cacheBytes = wordAudioRepository.cacheSizeBytes()
        val allVoicePacks = voicePackRepository.getAllVoicePacks()
        val voicePacksById = allVoicePacks.associateBy(VoicePack::id)
        val sources = pronunciationSourceRegistry?.refreshBuiltinSources()
            ?: pronunciationSourceRepository.getAllSources()
        val sourceItems = sources
            .sortedWith(
                compareByDescending<PronunciationSource> { it.isDefaultForWord }
                    .thenByDescending { it.isDefaultForLongText }
                    .thenBy { it.sortOrder }
                    .thenBy { it.id },
            )
            .map { source ->
                buildPronunciationSourceItemUiState(
                    source = source,
                    voicePacksById = voicePacksById,
                )
            }
        val voicePacks = allVoicePacks.map { pack ->
            buildVoicePackItemUiState(
                pack = pack,
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
            defaultWordSourceLabel = sourceItems.firstOrNull { it.isDefaultForWord }?.title ?: "未设置",
            defaultLongTextSourceLabel = sourceItems.firstOrNull { it.isDefaultForLongText }?.title ?: "未设置",
            sourceItems = sourceItems,
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

    suspend fun setDefaultWordSource(sourceId: String): PronunciationSettingsUiState {
        pronunciationSourceRegistry?.refreshBuiltinSources()
        val target = pronunciationSourceRepository.getSource(sourceId)
            ?: return loadUiState(errorMessage = "没有找到对应发音源。")
        if (!target.enabled) {
            return loadUiState(errorMessage = "该发音源当前不可用。")
        }
        pronunciationSourceRepository.setDefaultWordSource(sourceId)
        return loadUiState(statusMessage = "已将 ${target.name} 设为单词默认来源。")
    }

    suspend fun setDefaultLongTextSource(sourceId: String): PronunciationSettingsUiState {
        pronunciationSourceRegistry?.refreshBuiltinSources()
        val target = pronunciationSourceRepository.getSource(sourceId)
            ?: return loadUiState(errorMessage = "没有找到对应发音源。")
        if (!target.enabled) {
            return loadUiState(errorMessage = "该发音源当前不可用。")
        }
        pronunciationSourceRepository.setDefaultLongTextSource(sourceId)
        return loadUiState(statusMessage = "已将 ${target.name} 设为长文本默认来源。")
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
        return loadUiState(statusMessage = "已启用该口音的语音包。")
    }

    suspend fun downloadVoicePack(id: String): PronunciationSettingsUiState {
        val settings = settingsRepository.getSettings()
        voicePackDownloadController.enqueue(
            voicePackId = id,
            allowCellular = settings.allowCellularVoicePackDownload,
        )
        return loadUiState(statusMessage = "已开始下载并安装语音包。")
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
        val appContext = context.applicationContext
        val settingsRepository = buildSettingsRepository(appContext)
        val wordAudioRepository = buildWordAudioRepository(appContext)
        val voicePackRepository = buildVoicePackRepository(appContext)
        val pronunciationSourceRepository = buildPronunciationSourceRepository(appContext)
        PronunciationSettingsViewModel(
            settingsRepository = settingsRepository,
            wordAudioRepository = wordAudioRepository,
            voicePackRepository = voicePackRepository,
            voicePackDownloadController = buildVoicePackDownloadController(appContext),
            pronunciationSourceRepository = pronunciationSourceRepository,
            pronunciationSourceRegistry = PronunciationSourceRegistry(
                sourceRepository = pronunciationSourceRepository,
                voicePackRepository = voicePackRepository,
                settingsRepository = settingsRepository,
            ),
        )
    }

internal fun buildPronunciationSourceItemUiState(
    source: PronunciationSource,
    voicePacksById: Map<String, VoicePack>,
): PronunciationSourceItemUiState {
    val type = PronunciationSourceType.fromStorageValue(source.sourceType)
    val accentLabel = PronunciationAccent.fromStorageValue(source.accent).label
    val typeLabel = when (type) {
        PronunciationSourceType.DICTIONARY -> "词典音频"
        PronunciationSourceType.LOCAL_NATIVE -> "本地原生"
        PronunciationSourceType.LOCAL_BRIDGE -> "本地桥接"
        PronunciationSourceType.CLOUD_TTS -> "云端 TTS"
        null -> "未知来源"
    }
    val voicePack = source.backingVoicePackId?.let(voicePacksById::get)
    val detail = buildList {
        add(typeLabel)
        add(accentLabel)
        if (voicePack != null) {
            add(
                when (voicePack.status) {
                    VoicePackStatus.READY.storageValue -> "语音包已安装"
                    VoicePackStatus.BROKEN.storageValue -> "语音包异常"
                    VoicePackStatus.DOWNLOADING.storageValue,
                    VoicePackStatus.VERIFYING.storageValue,
                    VoicePackStatus.INSTALLING.storageValue -> "语音包处理中"
                    else -> "语音包未安装"
                },
            )
            if (voicePack.supportsImportedWords) {
                add("支持导入词书")
            }
        } else if (type == PronunciationSourceType.DICTIONARY) {
            add("内建来源")
        }
    }.joinToString(" · ")
    return PronunciationSourceItemUiState(
        id = source.id,
        title = source.name,
        subtitle = detail,
        typeLabel = typeLabel,
        availablePresetLabels = source.presets.map { preset ->
            if (preset.isDefaultPreset) "${preset.displayName}（默认）" else preset.displayName
        },
        isDefaultForWord = source.isDefaultForWord,
        isDefaultForLongText = source.isDefaultForLongText,
        canSetDefaultForWord = source.enabled && !source.isDefaultForWord,
        canSetDefaultForLongText = source.enabled && !source.isDefaultForLongText,
    )
}

internal fun buildVoicePackItemUiState(
    pack: VoicePack,
    failureReason: String? = null,
): VoicePackItemUiState {
    val statusLabel = when (pack.status) {
        VoicePackStatus.READY.storageValue -> "已安装"
        VoicePackStatus.DOWNLOADING.storageValue -> "下载中"
        VoicePackStatus.VERIFYING.storageValue -> "校验中"
        VoicePackStatus.INSTALLING.storageValue -> "安装中"
        VoicePackStatus.BROKEN.storageValue -> brokenStatusLabel(failureReason)
        else -> "未安装"
    }
    val isBusy = pack.status == VoicePackStatus.DOWNLOADING.storageValue ||
        pack.status == VoicePackStatus.VERIFYING.storageValue ||
        pack.status == VoicePackStatus.INSTALLING.storageValue
    val isReady = pack.status == VoicePackStatus.READY.storageValue
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
    val storageHint = pack.estimatedStorageBytes?.let { "%.2f MB".format(it / 1024f / 1024f) }
    val ramHint = pack.estimatedRamMb?.let { "${it} MB RAM" }
    val resourceHint = listOfNotNull(storageHint, ramHint).joinToString(" · ").ifBlank { "资源占用信息待补充" }
    return VoicePackItemUiState(
        id = pack.id,
        name = pack.name,
        locale = pack.locale,
        versionLabel = "v${pack.version}",
        engineLabel = engineLabel,
        capabilitySummary = capabilitySummary,
        resourceHint = resourceHint,
        statusLabel = statusLabel,
        failureReason = failureReason,
        isActive = pack.isActive,
        isBusy = isBusy,
        canActivate = isReady && !pack.isActive,
        canDownload = !isReady && !isBusy,
        canDelete = isReady || pack.status == VoicePackStatus.BROKEN.storageValue,
    )
}

private fun brokenStatusLabel(failureReason: String?): String {
    val reason = failureReason.orEmpty().lowercase()
    return when {
        listOf("download", "下载", "http", "network", "网络").any(reason::contains) -> "下载失败"
        listOf("verify", "checksum", "payload", "校验", "manifest id").any(reason::contains) -> "校验失败"
        listOf("runtime", "jni", "synthesis", "运行", "合成").any(reason::contains) -> "运行异常"
        else -> "安装失败"
    }
}
