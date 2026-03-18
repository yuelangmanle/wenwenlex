package com.yueliangmanle.danci.core.model

import java.time.Instant

data class AiProviderProfile(
    val id: String,
    val name: String,
    val providerType: String = "custom",
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
