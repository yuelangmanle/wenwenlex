package com.yueliangmanle.danci.core.data

import android.content.Context
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.AiProviderProfileDao
import com.yueliangmanle.danci.core.database.entity.AiProviderProfileEntity
import com.yueliangmanle.danci.core.model.AiProviderProfile

interface AiProfileRepository {
    suspend fun getProfiles(): List<AiProviderProfile>
    suspend fun getProfile(profileId: String): AiProviderProfile?
    suspend fun saveProfile(profile: AiProviderProfile)
    suspend fun deleteProfile(profileId: String)
}

class RoomAiProfileRepository(
    private val dao: AiProviderProfileDao,
) : AiProfileRepository {
    override suspend fun getProfiles(): List<AiProviderProfile> =
        dao.getAllProfiles().map(AiProviderProfileEntity::asExternalModel)

    override suspend fun getProfile(profileId: String): AiProviderProfile? =
        dao.getProfileById(profileId)?.asExternalModel()

    override suspend fun saveProfile(profile: AiProviderProfile) {
        dao.upsertProfile(profile.asEntity())
    }

    override suspend fun deleteProfile(profileId: String) {
        val entity = dao.getProfileById(profileId) ?: return
        dao.deleteProfile(entity)
    }
}

internal fun AiProviderProfileEntity.asExternalModel(): AiProviderProfile =
    AiProviderProfile(
        id = id,
        name = name,
        providerType = providerType,
        baseUrl = baseUrl,
        model = model,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastValidatedAt = lastValidatedAt,
    )

internal fun AiProviderProfile.asEntity(): AiProviderProfileEntity =
    AiProviderProfileEntity(
        id = id,
        name = name,
        providerType = providerType,
        baseUrl = baseUrl,
        model = model,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastValidatedAt = lastValidatedAt,
    )

fun buildAiProfileRepository(context: Context): AiProfileRepository =
    RoomAiProfileRepository(buildDanciDatabase(context.applicationContext).aiProviderProfileDao())
