package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.WordAudioAsset
import com.yueliangmanle.danci.core.security.AiCredentialStore

data class CloudTtsPreset(
    val id: String,
    val label: String,
    val voice: String,
    val promptPrefix: String? = null,
)

data class CloudTtsProviderConfig(
    val providerId: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val presetId: String,
)

data class CloudTtsHealthResult(
    val healthy: Boolean,
    val message: String,
)

data class CloudTtsSynthesisResult(
    val audioBytes: ByteArray,
    val mimeType: String,
    val providerLabel: String,
    val presetLabel: String,
)

interface CloudTtsProvider {
    val providerId: String
    val providerLabel: String

    fun presets(): List<CloudTtsPreset>

    suspend fun synthesize(
        text: String,
        accent: PronunciationAccent,
        config: CloudTtsProviderConfig,
    ): CloudTtsSynthesisResult?

    suspend fun checkHealth(config: CloudTtsProviderConfig): CloudTtsHealthResult
}

class CloudTtsProviderRegistry(
    private val providers: List<CloudTtsProvider>,
) {
    fun allProviders(): List<CloudTtsProvider> = providers

    fun findProvider(providerId: String?): CloudTtsProvider? {
        if (providerId.isNullOrBlank()) {
            return providers.firstOrNull()
        }
        return providers.firstOrNull { it.providerId == providerId }
    }
}

class CloudTtsApiHealthChecker(
    private val registry: CloudTtsProviderRegistry,
) {
    suspend fun check(config: CloudTtsProviderConfig): CloudTtsHealthResult {
        val provider = registry.findProvider(config.providerId)
            ?: return CloudTtsHealthResult(
                healthy = false,
                message = "没有找到对应的云端 TTS provider。",
            )
        return provider.checkHealth(config)
    }
}

interface CloudTtsEngineContract {
    suspend fun synthesizeWord(
        word: Word,
        accent: PronunciationAccent,
    ): WordAudioAsset?
}

object NoOpCloudTtsEngine : CloudTtsEngineContract {
    override suspend fun synthesizeWord(
        word: Word,
        accent: PronunciationAccent,
    ): WordAudioAsset? = null
}

class CloudTtsEngine(
    private val settingsRepository: SettingsRepository,
    private val credentialStore: AiCredentialStore,
    private val wordAudioRepository: WordAudioRepository,
    private val registry: CloudTtsProviderRegistry,
) : CloudTtsEngineContract {
    override suspend fun synthesizeWord(
        word: Word,
        accent: PronunciationAccent,
    ): WordAudioAsset? {
        val lemma = word.lemma.trim()
        if (lemma.isBlank()) {
            return null
        }
        wordAudioRepository.findCloudTtsAsset(word.id, accent)?.let { return it }

        val settings = settingsRepository.getSettings()
        val provider = registry.findProvider(settings.defaultCloudTtsProviderId) ?: return null
        val providerId = settings.defaultCloudTtsProviderId
            ?.takeIf(String::isNotBlank)
            ?: provider.providerId
        val presetId = settings.defaultCloudTtsPresetId
            ?.takeIf(String::isNotBlank)
            ?: provider.presets().firstOrNull()?.id
            ?: return null
        val apiKey = credentialStore.readApiKey(cloudTtsCredentialProfileId(providerId))
            ?: credentialStore.readApiKey()
        if (apiKey.isNullOrBlank()) {
            return null
        }
        val result = provider.synthesize(
            text = lemma,
            accent = accent,
            config = CloudTtsProviderConfig(
                providerId = providerId,
                baseUrl = settings.cloudTtsBaseUrl,
                model = settings.cloudTtsModel,
                apiKey = apiKey.trim(),
                presetId = presetId,
            ),
        ) ?: return null
        return wordAudioRepository.cacheCloudTtsAudio(
            wordId = word.id,
            accent = accent,
            normalizedWord = normalizeWordForAudioPath(lemma),
            audioBytes = result.audioBytes,
            mimeType = result.mimeType,
        )
    }
}

fun cloudTtsCredentialProfileId(providerId: String): String = "cloud-tts-$providerId"

internal fun normalizeWordForAudioPath(text: String): String =
    text
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9_-]+"), "_")
        .trim('_')
        .ifBlank { "word_audio" }
