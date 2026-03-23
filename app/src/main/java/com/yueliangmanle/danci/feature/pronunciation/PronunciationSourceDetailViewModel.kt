package com.yueliangmanle.danci.feature.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.AiProfileRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.buildAiProfileRepository
import com.yueliangmanle.danci.core.data.buildPronunciationSourceRepository
import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourcePreset
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.isMiMoTtsCompatible
import com.yueliangmanle.danci.core.pronunciation.ApiHealthChecker
import com.yueliangmanle.danci.core.pronunciation.CloudTtsHealthCheckResult
import com.yueliangmanle.danci.core.security.AiCredentialStore
import com.yueliangmanle.danci.core.security.buildAiCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PronunciationSourceProfileOptionUiState(
    val id: String,
    val title: String,
    val summary: String,
    val hasApiKey: Boolean,
)

data class PronunciationSourcePresetOptionUiState(
    val id: String,
    val title: String,
    val summary: String,
    val isDefault: Boolean,
    val supportsAdvancedStyle: Boolean,
)

data class PronunciationSourceDetailUiState(
    val isLoading: Boolean = false,
    val sourceId: String = "",
    val sourceTitle: String = "",
    val sourceSubtitle: String = "",
    val sourceType: String = "",
    val accentLabel: String = "",
    val isCloudSource: Boolean = false,
    val isLocalSource: Boolean = false,
    val selectedProfileId: String? = null,
    val profileOptions: List<PronunciationSourceProfileOptionUiState> = emptyList(),
    val selectedPresetId: String? = null,
    val presetOptions: List<PronunciationSourcePresetOptionUiState> = emptyList(),
    val advancedStyleText: String = "",
    val testText: String = "abandon",
    val canCheckApi: Boolean = false,
    val healthCheckSummary: String? = null,
    val healthCheckLatencyLabel: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class PronunciationSourceDetailViewModel(
    private val pronunciationSourceRepository: PronunciationSourceRepository,
    private val aiProfileRepository: AiProfileRepository,
    private val credentialStore: AiCredentialStore,
    private val apiHealthChecker: ApiHealthChecker,
) {
    suspend fun loadUiState(
        sourceId: String,
        statusMessage: String? = null,
        errorMessage: String? = null,
        healthCheckResult: CloudTtsHealthCheckResult? = null,
        advancedStyleTextOverride: String? = null,
        testTextOverride: String? = null,
    ): PronunciationSourceDetailUiState = withContext(Dispatchers.IO) {
        val source = pronunciationSourceRepository.getSource(sourceId)
            ?: return@withContext PronunciationSourceDetailUiState(
                errorMessage = "没有找到对应发音源。",
            )
        val profiles = aiProfileRepository.getProfiles()
            .filter(AiProviderProfile::enabled)
            .filter { it.isMiMoTtsCompatible() }
        val profileOptions = profiles.map { profile ->
            PronunciationSourceProfileOptionUiState(
                id = profile.id,
                title = profile.name,
                summary = "${profile.model} · ${profile.baseUrl}",
                hasApiKey = !credentialStore.readApiKey(profile.id).isNullOrBlank(),
            )
        }
        val selectedPreset = source.presets.firstOrNull(PronunciationSourcePreset::isDefaultPreset)
            ?: source.presets.firstOrNull()
        val selectedProfileId = source.providerProfileId
            ?.takeIf { profileId -> profileOptions.any { it.id == profileId } }
            ?: profileOptions.firstOrNull()?.id
        val type = PronunciationSourceType.fromStorageValue(source.sourceType)
        PronunciationSourceDetailUiState(
            sourceId = source.id,
            sourceTitle = source.name,
            sourceSubtitle = buildSourceSubtitle(source),
            sourceType = source.sourceType,
            accentLabel = com.yueliangmanle.danci.core.model.PronunciationAccent.fromStorageValue(source.accent).label,
            isCloudSource = type == PronunciationSourceType.CLOUD_TTS,
            isLocalSource = type == PronunciationSourceType.LOCAL_NATIVE || type == PronunciationSourceType.LOCAL_BRIDGE,
            selectedProfileId = selectedProfileId,
            profileOptions = profileOptions,
            selectedPresetId = selectedPreset?.presetId,
            presetOptions = source.presets.map { preset ->
                PronunciationSourcePresetOptionUiState(
                    id = preset.presetId,
                    title = preset.displayName,
                    summary = listOfNotNull(
                        preset.voice.takeIf(String::isNotBlank),
                        preset.styleTemplate?.takeIf(String::isNotBlank),
                    ).joinToString(" · "),
                    isDefault = preset.isDefaultPreset,
                    supportsAdvancedStyle = preset.advancedStyleEnabled,
                )
            },
            advancedStyleText = advancedStyleTextOverride ?: selectedPreset
                ?.takeIf(PronunciationSourcePreset::advancedStyleEnabled)
                ?.styleTemplate
                .orEmpty(),
            testText = testTextOverride ?: "abandon",
            canCheckApi = selectedProfileId?.let { profileId ->
                profileOptions.any { it.id == profileId && it.hasApiKey }
            } == true,
            healthCheckSummary = healthCheckResult?.summary,
            healthCheckLatencyLabel = healthCheckResult?.latencyMs?.let { "耗时 ${it} ms" },
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    suspend fun bindProfile(
        sourceId: String,
        profileId: String,
    ): PronunciationSourceDetailUiState = withContext(Dispatchers.IO) {
        val source = pronunciationSourceRepository.getSource(sourceId)
            ?: return@withContext loadUiState(sourceId, errorMessage = "没有找到对应发音源。")
        pronunciationSourceRepository.upsertSources(
            listOf(
                source.copy(providerProfileId = profileId),
            ),
        )
        loadUiState(sourceId, statusMessage = "已绑定云端 TTS 档案。")
    }

    suspend fun selectPreset(
        sourceId: String,
        presetId: String,
    ): PronunciationSourceDetailUiState = withContext(Dispatchers.IO) {
        val source = pronunciationSourceRepository.getSource(sourceId)
            ?: return@withContext loadUiState(sourceId, errorMessage = "没有找到对应发音源。")
        pronunciationSourceRepository.upsertSources(
            listOf(
                source.copy(
                    presets = source.presets.map { preset ->
                        preset.copy(isDefaultPreset = preset.presetId == presetId)
                    },
                ),
            ),
        )
        loadUiState(sourceId, statusMessage = "已切换默认预设。")
    }

    suspend fun checkApi(
        sourceId: String,
        current: PronunciationSourceDetailUiState,
    ): PronunciationSourceDetailUiState = withContext(Dispatchers.IO) {
        val source = pronunciationSourceRepository.getSource(sourceId)
            ?: return@withContext loadUiState(sourceId, errorMessage = "没有找到对应发音源。")
        val profileId = current.selectedProfileId
            ?: return@withContext loadUiState(sourceId, errorMessage = "请先绑定一条 MiMo 档案。")
        val profile = aiProfileRepository.getProfile(profileId)
            ?: return@withContext loadUiState(sourceId, errorMessage = "绑定的 AI 档案不存在。")
        val apiKey = credentialStore.readApiKey(profileId)
            ?: return@withContext loadUiState(sourceId, errorMessage = "当前档案还没有保存 API Key。")
        val preset = source.presets.firstOrNull { it.presetId == current.selectedPresetId }
            ?: source.presets.firstOrNull(PronunciationSourcePreset::isDefaultPreset)
            ?: source.presets.firstOrNull()
        val voice = preset?.voice?.takeIf(String::isNotBlank) ?: "default_en"
        val style = current.advancedStyleText.trim().takeIf(String::isNotBlank)
            ?: preset?.styleTemplate
        val healthCheckResult = apiHealthChecker.check(
            profile = profile,
            apiKey = apiKey,
            voice = voice,
            text = current.testText.trim().ifBlank { "abandon" },
            style = style,
        )
        loadUiState(
            sourceId = sourceId,
            statusMessage = if (healthCheckResult.success) "API 检测完成。" else null,
            errorMessage = if (healthCheckResult.success) null else "API 检测失败。",
            healthCheckResult = healthCheckResult,
            advancedStyleTextOverride = current.advancedStyleText,
            testTextOverride = current.testText,
        )
    }

    fun updateAdvancedStyle(
        current: PronunciationSourceDetailUiState,
        text: String,
    ): PronunciationSourceDetailUiState = current.copy(
        advancedStyleText = text,
        statusMessage = null,
        errorMessage = null,
    )

    fun updateTestText(
        current: PronunciationSourceDetailUiState,
        text: String,
    ): PronunciationSourceDetailUiState = current.copy(
        testText = text,
        statusMessage = null,
        errorMessage = null,
    )
}

private fun buildSourceSubtitle(source: PronunciationSource): String =
    when (PronunciationSourceType.fromStorageValue(source.sourceType)) {
        PronunciationSourceType.DICTIONARY -> "词典音频 · ${com.yueliangmanle.danci.core.model.PronunciationAccent.fromStorageValue(source.accent).label}"
        PronunciationSourceType.LOCAL_NATIVE -> "本地原生离线发音"
        PronunciationSourceType.LOCAL_BRIDGE -> "本地桥接语音包"
        PronunciationSourceType.CLOUD_TTS -> "云端 TTS · 支持 MiMo 预设与 API 检测"
        null -> "未知来源"
    }

suspend fun loadPronunciationSourceDetailViewModel(
    context: Context,
): PronunciationSourceDetailViewModel = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    PronunciationSourceDetailViewModel(
        pronunciationSourceRepository = buildPronunciationSourceRepository(appContext),
        aiProfileRepository = buildAiProfileRepository(appContext),
        credentialStore = buildAiCredentialStore(appContext),
        apiHealthChecker = ApiHealthChecker(),
    )
}
