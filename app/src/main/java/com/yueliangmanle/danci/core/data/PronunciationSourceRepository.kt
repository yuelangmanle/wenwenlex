package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.PronunciationSourceDao
import com.yueliangmanle.danci.core.database.entity.PronunciationSourceEntity
import com.yueliangmanle.danci.core.database.entity.PronunciationSourcePresetEntity
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourcePreset

interface PronunciationSourceRepository {
    suspend fun getAllSources(): List<PronunciationSource>
    suspend fun getSource(sourceId: String): PronunciationSource?
    suspend fun upsertSources(sources: List<PronunciationSource>)
    suspend fun setDefaultWordSource(sourceId: String)
    suspend fun setDefaultLongTextSource(sourceId: String)
    suspend fun clearAll()
}

class RoomPronunciationSourceRepository(
    private val dao: PronunciationSourceDao,
) : PronunciationSourceRepository {
    override suspend fun getAllSources(): List<PronunciationSource> {
        val sources = dao.getAllSources()
        if (sources.isEmpty()) {
            return emptyList()
        }
        return sources.map { source ->
            val presets = dao.getPresetsBySource(source.id).map(PronunciationSourcePresetEntity::asExternalModel)
            source.asExternalModel(presets)
        }
    }

    override suspend fun getSource(sourceId: String): PronunciationSource? =
        getAllSources().firstOrNull { it.id == sourceId }

    override suspend fun upsertSources(sources: List<PronunciationSource>) {
        if (sources.isEmpty()) {
            return
        }
        dao.upsertSources(sources.map(PronunciationSource::asEntity))
        dao.deletePresetsBySourceIds(sources.map(PronunciationSource::id))
        val presets = sources.flatMap { source ->
            source.presets.map { preset -> preset.asEntity(sourceId = source.id) }
        }
        if (presets.isNotEmpty()) {
            dao.upsertPresets(presets)
        }
    }

    override suspend fun setDefaultWordSource(sourceId: String) {
        val currentSources = getAllSources()
        if (currentSources.none { it.id == sourceId }) {
            return
        }
        upsertSources(
            currentSources.map { source ->
                source.copy(isDefaultForWord = source.id == sourceId)
            },
        )
    }

    override suspend fun setDefaultLongTextSource(sourceId: String) {
        val currentSources = getAllSources()
        if (currentSources.none { it.id == sourceId }) {
            return
        }
        upsertSources(
            currentSources.map { source ->
                source.copy(isDefaultForLongText = source.id == sourceId)
            },
        )
    }

    override suspend fun clearAll() {
        dao.clearPresets()
        dao.clearSources()
    }
}

internal fun PronunciationSource.asEntity(): PronunciationSourceEntity =
    PronunciationSourceEntity(
        id = id,
        name = name,
        sourceType = sourceType,
        accent = accent,
        enabled = enabled,
        isDefaultForWord = isDefaultForWord,
        isDefaultForLongText = isDefaultForLongText,
        providerProfileId = providerProfileId,
        backingVoicePackId = backingVoicePackId,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun PronunciationSourceEntity.asExternalModel(
    presets: List<PronunciationSourcePreset>,
): PronunciationSource =
    PronunciationSource(
        id = id,
        name = name,
        sourceType = sourceType,
        accent = accent,
        enabled = enabled,
        isDefaultForWord = isDefaultForWord,
        isDefaultForLongText = isDefaultForLongText,
        providerProfileId = providerProfileId,
        backingVoicePackId = backingVoicePackId,
        sortOrder = sortOrder,
        presets = presets,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun PronunciationSourcePreset.asEntity(sourceId: String): PronunciationSourcePresetEntity =
    PronunciationSourcePresetEntity(
        sourceId = sourceId,
        presetId = presetId,
        displayName = displayName,
        voice = voice,
        styleTemplate = styleTemplate,
        advancedStyleEnabled = advancedStyleEnabled,
        isDefaultPreset = isDefaultPreset,
    )

internal fun PronunciationSourcePresetEntity.asExternalModel(): PronunciationSourcePreset =
    PronunciationSourcePreset(
        sourceId = sourceId,
        presetId = presetId,
        displayName = displayName,
        voice = voice,
        styleTemplate = styleTemplate,
        advancedStyleEnabled = advancedStyleEnabled,
        isDefaultPreset = isDefaultPreset,
    )

fun buildPronunciationSourceRepository(context: Context): PronunciationSourceRepository =
    RoomPronunciationSourceRepository(buildDanciDatabase(context.applicationContext).pronunciationSourceDao())
