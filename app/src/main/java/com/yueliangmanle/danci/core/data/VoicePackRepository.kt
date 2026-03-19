package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.room.withTransaction
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.VoicePackDao
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.io.File
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

interface VoicePackRepository {
    suspend fun getAllVoicePacks(): List<VoicePack>
    suspend fun getActiveVoicePack(): VoicePack?
    suspend fun activateVoicePack(id: String)
    suspend fun upsertVoicePack(voicePack: VoicePack)
    suspend fun removeVoicePack(id: String)
    suspend fun syncManifest(jsonText: String): Int
    fun voicePackRootDir(): File
}

class RoomVoicePackRepository(
    private val appContext: Context,
    private val dao: VoicePackDao,
    private val database: com.yueliangmanle.danci.core.database.DanciDatabase,
) : VoicePackRepository {
    override suspend fun getAllVoicePacks(): List<VoicePack> =
        dao.getAllVoicePacks().map(VoicePackEntity::asExternalModel)

    override suspend fun getActiveVoicePack(): VoicePack? =
        dao.getActiveVoicePack()?.asExternalModel()

    override suspend fun activateVoicePack(id: String) {
        database.withTransaction {
            dao.activateVoicePack(id)
        }
    }

    override suspend fun upsertVoicePack(voicePack: VoicePack) {
        dao.upsertVoicePack(voicePack.asEntity())
    }

    override suspend fun removeVoicePack(id: String) {
        dao.getVoicePackById(id)?.installDir?.let(::File)?.takeIf(File::exists)?.deleteRecursively()
        dao.deleteVoicePackById(id)
    }

    override suspend fun syncManifest(jsonText: String): Int {
        val root = JSONObject(jsonText)
        val items = root.optJSONArray("voicePacks") ?: JSONArray()
        val now = Instant.now()
        val currentActiveId = dao.getActiveVoicePack()?.id
        val voicePacks = mutableListOf<VoicePack>()
        repeat(items.length()) { index ->
            val item = items.getJSONObject(index)
            val existing = dao.getVoicePackById(item.getString("id"))
            voicePacks += VoicePack(
                id = item.getString("id"),
                name = item.optString("name").ifBlank { item.getString("id") },
                locale = item.optString("locale").ifBlank { "en-US" },
                accent = item.optString("accent").ifBlank { "auto" },
                engineType = item.optString("engineType").ifBlank { "sherpa_onnx" },
                version = item.optString("version").ifBlank { "1" },
                downloadUrl = item.optString("downloadUrl").ifBlank { null },
                manifestUrl = item.optString("manifestUrl").ifBlank { null },
                installDir = existing?.installDir,
                archiveChecksum = item.optString("archiveChecksum").ifBlank { null },
                installedSizeBytes = existing?.installedSizeBytes ?: 0L,
                status = existing?.status ?: VoicePackStatus.NOT_INSTALLED.storageValue,
                isActive = currentActiveId == item.getString("id"),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        }
        if (voicePacks.isNotEmpty()) {
            dao.upsertVoicePacks(voicePacks.map(VoicePack::asEntity))
        }
        return voicePacks.size
    }

    override fun voicePackRootDir(): File = File(appContext.filesDir, "voice-packs")
}

internal fun VoicePackEntity.asExternalModel(): VoicePack =
    VoicePack(
        id = id,
        name = name,
        locale = locale,
        accent = accent,
        engineType = engineType,
        version = version,
        downloadUrl = downloadUrl,
        manifestUrl = manifestUrl,
        installDir = installDir,
        archiveChecksum = archiveChecksum,
        installedSizeBytes = installedSizeBytes,
        status = status,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun VoicePack.asEntity(): VoicePackEntity =
    VoicePackEntity(
        id = id,
        name = name,
        locale = locale,
        accent = accent,
        engineType = engineType,
        version = version,
        downloadUrl = downloadUrl,
        manifestUrl = manifestUrl,
        installDir = installDir,
        archiveChecksum = archiveChecksum,
        installedSizeBytes = installedSizeBytes,
        status = status,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun buildVoicePackRepository(context: Context): VoicePackRepository {
    val appContext = context.applicationContext
    val database = buildDanciDatabase(appContext)
    return RoomVoicePackRepository(
        appContext = appContext,
        dao = database.voicePackDao(),
        database = database,
    )
}
