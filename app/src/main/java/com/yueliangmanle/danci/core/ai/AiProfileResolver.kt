package com.yueliangmanle.danci.core.ai

import android.content.Context
import com.yueliangmanle.danci.core.data.AiProfileRepository
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.DEFAULT_AI_BASE_URL
import com.yueliangmanle.danci.core.data.DEFAULT_AI_MODEL
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.buildAiProfileRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.model.AiCapability
import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.security.AiCredentialStore
import com.yueliangmanle.danci.core.security.buildAiCredentialStore
import java.time.Instant
import java.util.UUID

class AiProfileResolver {
    fun resolveProfileId(
        settings: AppSettings,
        capability: AiCapability,
    ): String? =
        when (capability) {
            AiCapability.WORD_HELP -> settings.wordHelpProfileId ?: settings.defaultAiProfileId
            AiCapability.PLAN_ADJUSTMENT -> settings.planAdjustmentProfileId ?: settings.defaultAiProfileId
            AiCapability.PHONETIC_FILL -> settings.phoneticFillProfileId ?: settings.defaultAiProfileId
        }
}

suspend fun ensureLegacyAiProfileMigrated(
    settingsRepository: SettingsRepository,
    profileRepository: AiProfileRepository,
    credentialStore: AiCredentialStore,
): AiProviderProfile? {
    val settings = settingsRepository.getSettings()
    val existingProfiles = profileRepository.getProfiles()
    if (existingProfiles.isNotEmpty() && !settings.defaultAiProfileId.isNullOrBlank()) {
        return existingProfiles.firstOrNull { it.id == settings.defaultAiProfileId }
    }

    val legacyApiKey = credentialStore.readApiKey()
    val shouldSeedProfile = settings.aiEnabled ||
        !legacyApiKey.isNullOrBlank() ||
        settings.aiBaseUrl != DEFAULT_AI_BASE_URL ||
        settings.aiModel != DEFAULT_AI_MODEL

    if (!shouldSeedProfile) {
        return existingProfiles.firstOrNull()
    }

    val profileId = settings.defaultAiProfileId ?: "legacy-${UUID.randomUUID().toString().take(8)}"
    val now = Instant.now()
    val profile = AiProviderProfile(
        id = profileId,
        name = "默认 API",
        providerType = "legacy",
        baseUrl = settings.aiBaseUrl,
        model = settings.aiModel,
        enabled = true,
        createdAt = now,
        updatedAt = now,
    )
    profileRepository.saveProfile(profile)
    if (!legacyApiKey.isNullOrBlank()) {
        credentialStore.saveApiKey(profileId, legacyApiKey)
    }
    settingsRepository.updateDefaultAiProfileId(profileId)
    return profile
}

suspend fun resolveRuntimeSettingsForCapability(
    context: Context,
    capability: AiCapability,
): AiRuntimeSettings? {
    val appContext = context.applicationContext
    val settingsRepository = buildSettingsRepository(appContext)
    val profileRepository = buildAiProfileRepository(appContext)
    val credentialStore = buildAiCredentialStore(appContext)
    val settings = settingsRepository.getSettings()
    ensureLegacyAiProfileMigrated(settingsRepository, profileRepository, credentialStore)
    val resolvedSettings = settingsRepository.getSettings()
    val profileId = AiProfileResolver().resolveProfileId(resolvedSettings, capability)
    val profile = if (profileId.isNullOrBlank()) {
        null
    } else {
        profileRepository.getProfile(profileId)
    }

    return if (profile != null) {
        AiRuntimeSettings(
            enabled = resolvedSettings.aiEnabled && profile.enabled,
            baseUrl = profile.baseUrl,
            apiKey = credentialStore.readApiKey(profile.id),
            model = profile.model,
        )
    } else {
        buildRuntimeSettings(resolvedSettings, credentialStore)
    }
}
