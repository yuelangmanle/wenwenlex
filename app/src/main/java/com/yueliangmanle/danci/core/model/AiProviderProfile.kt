package com.yueliangmanle.danci.core.model

import java.time.Instant

data class AiProviderProfile(
    val id: String,
    val name: String,
    val providerType: String = AI_PROVIDER_TYPE_CUSTOM,
    val baseUrl: String,
    val model: String,
    val enabled: Boolean = true,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val lastValidatedAt: Instant? = null,
)

enum class AiCapability {
    WORD_HELP,
    PLAN_ADJUSTMENT,
    PHONETIC_FILL,
}

fun AiProviderProfile.isMiMoTtsCompatible(): Boolean {
    val normalizedBaseUrl = baseUrl.trim().lowercase()
    val normalizedModel = model.trim().lowercase()
    val normalizedProviderType = providerType.trim().lowercase()
    return normalizedProviderType == AI_PROVIDER_TYPE_MIMO_TTS ||
        normalizedModel == MIMO_TTS_MODEL ||
        "xiaomimimo.com" in normalizedBaseUrl
}

const val AI_PROVIDER_TYPE_CUSTOM = "custom"
const val AI_PROVIDER_TYPE_MIMO_TTS = "mimo_tts"
const val MIMO_TTS_BASE_URL = "https://api.xiaomimimo.com/v1"
const val MIMO_TTS_MODEL = "mimo-v2-tts"
