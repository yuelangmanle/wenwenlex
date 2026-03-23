package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationMode
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import java.time.Instant

class PronunciationSourceRegistry(
    private val sourceRepository: PronunciationSourceRepository,
    private val voicePackRepository: VoicePackRepository,
    private val settingsRepository: SettingsRepository,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    suspend fun refreshBuiltinSources(): List<PronunciationSource> {
        val existingSources = sourceRepository.getAllSources()
        val settings = settingsRepository.getSettings()
        val readyVoicePacks = voicePackRepository.getSourceReadyVoicePacks()
        val existingById = existingSources.associateBy(PronunciationSource::id)
        val now = nowProvider()

        val updates = mutableListOf<PronunciationSource>()
        updates += listOf(
            buildDictionarySource(
                accent = PronunciationAccent.UK,
                sortOrder = 100,
                existing = existingById[DICTIONARY_UK_SOURCE_ID],
                now = now,
            ),
            buildDictionarySource(
                accent = PronunciationAccent.US,
                sortOrder = 110,
                existing = existingById[DICTIONARY_US_SOURCE_ID],
                now = now,
            ),
        )
        updates += readyVoicePacks.mapIndexedNotNull { index, voicePack ->
            buildVoicePackSource(
                voicePack = voicePack,
                sortOrder = 200 + index,
                existing = existingById[voicePack.id],
                now = now,
            )
        }

        if (readyVoicePacks.isNotEmpty()) {
            val activeBuiltinLocalIds = updates
                .asSequence()
                .filter { source ->
                    PronunciationSourceType.fromStorageValue(source.sourceType) in BUILTIN_LOCAL_SOURCE_TYPES
                }
                .map(PronunciationSource::id)
                .toSet()
            updates += existingSources
                .asSequence()
                .filter(::isBuiltinLocalSource)
                .filterNot { it.id in activeBuiltinLocalIds }
                .map { source ->
                    source.copy(
                        enabled = false,
                        isDefaultForWord = false,
                        isDefaultForLongText = false,
                        updatedAt = now,
                    )
                }
                .toList()
        }

        if (updates.isNotEmpty()) {
            sourceRepository.upsertSources(updates)
        }

        var refreshedSources = sourceRepository.getAllSources()
        if (refreshedSources.none { it.enabled && it.isDefaultForWord }) {
            val defaultWordSourceId = seedDefaultWordSourceId(
                sources = refreshedSources,
                settings = settings,
            )
            if (defaultWordSourceId != null) {
                sourceRepository.setDefaultWordSource(defaultWordSourceId)
            }
            refreshedSources = sourceRepository.getAllSources()
        }
        if (refreshedSources.none { it.enabled && it.isDefaultForLongText }) {
            val defaultLongTextSourceId = seedDefaultLongTextSourceId(
                sources = refreshedSources,
                settings = settings,
            )
            if (defaultLongTextSourceId != null) {
                sourceRepository.setDefaultLongTextSource(defaultLongTextSourceId)
            }
            refreshedSources = sourceRepository.getAllSources()
        }
        return refreshedSources
    }

    suspend fun resolveCurrentWordSource(
        accent: PronunciationAccent,
    ): PronunciationSource {
        val settings = settingsRepository.getSettings()
        val sessionPreference = settingsRepository.getPronunciationSessionPreference()
        val sources = refreshBuiltinSources()
        val requestedAccent = resolveRequestedAccent(
            requestedAccent = accent,
            settings = settings,
        )
        return resolveSource(
            sources = sources,
            requestedAccent = requestedAccent,
            preferredSourceId = sessionPreference.sessionWordPronunciationSourceId,
            defaultSelector = PronunciationSource::isDefaultForWord,
        )
    }

    suspend fun resolveCurrentLongTextSource(
        accent: PronunciationAccent,
    ): PronunciationSource {
        val settings = settingsRepository.getSettings()
        val sessionPreference = settingsRepository.getPronunciationSessionPreference()
        val sources = refreshBuiltinSources()
        val requestedAccent = resolveRequestedAccent(
            requestedAccent = accent,
            settings = settings,
        )
        return resolveSource(
            sources = sources,
            requestedAccent = requestedAccent,
            preferredSourceId = sessionPreference.sessionLongTextPronunciationSourceId,
            defaultSelector = PronunciationSource::isDefaultForLongText,
        )
    }

    suspend fun updateSessionWordSource(sourceId: String?) {
        settingsRepository.updateSessionWordPronunciationSourceId(sourceId)
    }

    suspend fun updateSessionLongTextSource(sourceId: String?) {
        settingsRepository.updateSessionLongTextPronunciationSourceId(sourceId)
    }

    private fun resolveSource(
        sources: List<PronunciationSource>,
        requestedAccent: PronunciationAccent,
        preferredSourceId: String?,
        defaultSelector: (PronunciationSource) -> Boolean,
    ): PronunciationSource {
        val enabledSources = sources.filter(PronunciationSource::enabled)
        val sessionSource = preferredSourceId
            ?.let { sourceId -> enabledSources.firstOrNull { it.id == sourceId } }
            ?.let { adaptAccent(enabledSources, it, requestedAccent) }
        if (sessionSource != null) {
            return sessionSource
        }

        val defaultSource = enabledSources
            .firstOrNull(defaultSelector)
            ?.let { adaptAccent(enabledSources, it, requestedAccent) }
        if (defaultSource != null) {
            return defaultSource
        }

        val dictionaryFallback = enabledSources.firstOrNull { source ->
            source.id == dictionarySourceId(requestedAccent)
        }
        if (dictionaryFallback != null) {
            return dictionaryFallback
        }

        return enabledSources
            .firstOrNull { source ->
                PronunciationAccent.fromStorageValue(source.accent) == requestedAccent
            }
            ?: enabledSources.firstOrNull()
            ?: buildDictionarySource(
                accent = requestedAccent,
                sortOrder = 100,
                existing = null,
                now = nowProvider(),
            )
    }

    private fun adaptAccent(
        sources: List<PronunciationSource>,
        source: PronunciationSource,
        requestedAccent: PronunciationAccent,
    ): PronunciationSource {
        val sourceAccent = PronunciationAccent.fromStorageValue(source.accent)
        if (requestedAccent == PronunciationAccent.AUTO ||
            sourceAccent == requestedAccent ||
            sourceAccent == PronunciationAccent.AUTO
        ) {
            return source
        }

        val sourceType = PronunciationSourceType.fromStorageValue(source.sourceType)
        return when (sourceType) {
            PronunciationSourceType.DICTIONARY -> {
                sources.firstOrNull { candidate ->
                    candidate.id == dictionarySourceId(requestedAccent) && candidate.enabled
                } ?: source
            }
            PronunciationSourceType.LOCAL_NATIVE,
            PronunciationSourceType.LOCAL_BRIDGE,
            PronunciationSourceType.CLOUD_TTS,
            null -> {
                sources.firstOrNull { candidate ->
                    candidate.enabled &&
                        PronunciationSourceType.fromStorageValue(candidate.sourceType) == sourceType &&
                        PronunciationAccent.fromStorageValue(candidate.accent) == requestedAccent
                } ?: source
            }
        }
    }

    private fun buildDictionarySource(
        accent: PronunciationAccent,
        sortOrder: Int,
        existing: PronunciationSource?,
        now: Instant,
    ): PronunciationSource {
        val sourceId = dictionarySourceId(accent)
        return PronunciationSource(
            id = sourceId,
            name = "词典发音（${accent.label}）",
            sourceType = PronunciationSourceType.DICTIONARY.storageValue,
            accent = accent.storageValue,
            enabled = true,
            isDefaultForWord = existing?.isDefaultForWord == true,
            isDefaultForLongText = existing?.isDefaultForLongText == true,
            providerProfileId = existing?.providerProfileId,
            backingVoicePackId = null,
            sortOrder = existing?.sortOrder ?: sortOrder,
            presets = existing?.presets.orEmpty(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
    }

    private fun buildVoicePackSource(
        voicePack: VoicePack,
        sortOrder: Int,
        existing: PronunciationSource?,
        now: Instant,
    ): PronunciationSource? {
        val sourceType = when (VoicePackEngineType.fromStorageValue(voicePack.engineType)) {
            VoicePackEngineType.SHERPA_ONNX -> PronunciationSourceType.LOCAL_NATIVE
            VoicePackEngineType.SYSTEM_TTS_BRIDGE -> PronunciationSourceType.LOCAL_BRIDGE
        }
        return PronunciationSource(
            id = voicePack.id,
            name = voicePack.name,
            sourceType = sourceType.storageValue,
            accent = voicePack.accent,
            enabled = true,
            isDefaultForWord = existing?.isDefaultForWord == true,
            isDefaultForLongText = existing?.isDefaultForLongText == true,
            providerProfileId = existing?.providerProfileId,
            backingVoicePackId = voicePack.id,
            sortOrder = existing?.sortOrder ?: sortOrder,
            presets = existing?.presets.orEmpty(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
    }

    private fun isBuiltinLocalSource(source: PronunciationSource): Boolean =
        source.backingVoicePackId != null &&
            PronunciationSourceType.fromStorageValue(source.sourceType) in BUILTIN_LOCAL_SOURCE_TYPES

    private fun seedDefaultWordSourceId(
        sources: List<PronunciationSource>,
        settings: AppSettings,
    ): String? {
        val activeVoicePackId = settings.activeVoicePackId
        val pronunciationMode = PronunciationMode.fromStorageValue(settings.pronunciationMode)
        val preferredAccent = resolveRequestedAccent(
            requestedAccent = PronunciationAccent.fromStorageValue(settings.preferredPronunciationAccent),
            settings = settings,
        )
        if (pronunciationMode == PronunciationMode.OFFLINE_FIRST) {
            activeVoicePackId
                ?.takeIf { sourceId -> sources.any { it.id == sourceId && it.enabled } }
                ?.let { return it }
            sources.firstOrNull { source ->
                source.enabled &&
                    PronunciationSourceType.fromStorageValue(source.sourceType) in BUILTIN_LOCAL_SOURCE_TYPES &&
                    PronunciationAccent.fromStorageValue(source.accent) == preferredAccent
            }?.let { return it.id }
        }
        return dictionarySourceId(preferredAccent)
            .takeIf { sourceId -> sources.any { it.id == sourceId && it.enabled } }
            ?: sources.firstOrNull(PronunciationSource::enabled)?.id
    }

    private fun seedDefaultLongTextSourceId(
        sources: List<PronunciationSource>,
        settings: AppSettings,
    ): String? {
        val preferredAccent = resolveRequestedAccent(
            requestedAccent = PronunciationAccent.fromStorageValue(settings.preferredPronunciationAccent),
            settings = settings,
        )
        if (settings.preferOfflineForLongText) {
            settings.activeVoicePackId
                ?.takeIf { sourceId -> sources.any { it.id == sourceId && it.enabled } }
                ?.let { return it }
            sources.firstOrNull { source ->
                source.enabled &&
                    PronunciationSourceType.fromStorageValue(source.sourceType) in BUILTIN_LOCAL_SOURCE_TYPES &&
                    PronunciationAccent.fromStorageValue(source.accent) == preferredAccent
            }?.let { return it.id }
        }
        return dictionarySourceId(preferredAccent)
            .takeIf { sourceId -> sources.any { it.id == sourceId && it.enabled } }
            ?: sources.firstOrNull(PronunciationSource::enabled)?.id
    }

    private fun resolveRequestedAccent(
        requestedAccent: PronunciationAccent,
        settings: AppSettings,
    ): PronunciationAccent =
        when (requestedAccent) {
            PronunciationAccent.AUTO -> {
                val stored = PronunciationAccent.fromStorageValue(settings.preferredPronunciationAccent)
                if (stored == PronunciationAccent.AUTO) {
                    PronunciationAccent.UK
                } else {
                    stored
                }
            }
            else -> requestedAccent
        }

    companion object {
        const val DICTIONARY_UK_SOURCE_ID = "dictionary-uk"
        const val DICTIONARY_US_SOURCE_ID = "dictionary-us"

        fun dictionarySourceId(accent: PronunciationAccent): String =
            when (accent) {
                PronunciationAccent.US -> DICTIONARY_US_SOURCE_ID
                PronunciationAccent.UK,
                PronunciationAccent.AUTO -> DICTIONARY_UK_SOURCE_ID
            }

        private val BUILTIN_LOCAL_SOURCE_TYPES = setOf(
            PronunciationSourceType.LOCAL_NATIVE,
            PronunciationSourceType.LOCAL_BRIDGE,
        )
    }
}
