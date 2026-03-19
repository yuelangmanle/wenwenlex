package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.room.withTransaction
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.VoicePackDao
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.io.File
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

private const val VOICE_PACK_MANIFEST_ASSET_PATH = "pronunciation/voice-pack-manifest.json"

interface VoicePackRepository {
    suspend fun getAllVoicePacks(): List<VoicePack>
    suspend fun getVoicePack(id: String): VoicePack?
    suspend fun getActiveVoicePack(): VoicePack?
    suspend fun activateVoicePack(id: String)
    suspend fun upsertVoicePack(voicePack: VoicePack)
    suspend fun removeVoicePack(id: String)
    suspend fun syncManifest(jsonText: String): Int
    suspend fun refreshCatalog(): Int
    suspend fun updateVoicePackStatus(
        id: String,
        status: String,
        installDir: String? = null,
        installedSizeBytes: Long? = null,
    )
    suspend fun markInstalled(
        id: String,
        installDir: String,
        installedSizeBytes: Long,
    )
    fun voicePackRootDir(): File
}

class RoomVoicePackRepository(
    private val appContext: Context,
    private val dao: VoicePackDao,
    private val database: com.yueliangmanle.danci.core.database.DanciDatabase,
) : VoicePackRepository {
    override suspend fun getAllVoicePacks(): List<VoicePack> =
        dao.getAllVoicePacks().map(VoicePackEntity::asExternalModel)

    override suspend fun getVoicePack(id: String): VoicePack? =
        dao.getVoicePackById(id)?.asExternalModel()

    override suspend fun getActiveVoicePack(): VoicePack? =
        dao.getActiveVoicePack()?.asExternalModel()

    override suspend fun activateVoicePack(id: String) {
        val target = dao.getVoicePackById(id) ?: return
        if (target.status != VoicePackStatus.READY.storageValue) {
            return
        }
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
        val now = Instant.now()
        val existingById = dao.getAllVoicePacks()
            .map(VoicePackEntity::asExternalModel)
            .associateBy(VoicePack::id)
        val currentActiveId = dao.getActiveVoicePack()?.id
        val voicePacks = parseVoicePackManifest(
            jsonText = jsonText,
            existingById = existingById,
            currentActiveId = currentActiveId,
            now = now,
        )
        if (voicePacks.isNotEmpty()) {
            dao.upsertVoicePacks(voicePacks.map(VoicePack::asEntity))
        }
        return voicePacks.size
    }

    override suspend fun refreshCatalog(): Int {
        val jsonText = appContext.assets.open(VOICE_PACK_MANIFEST_ASSET_PATH).bufferedReader().use { it.readText() }
        return syncManifest(jsonText)
    }

    override suspend fun updateVoicePackStatus(
        id: String,
        status: String,
        installDir: String?,
        installedSizeBytes: Long?,
    ) {
        val existing = dao.getVoicePackById(id) ?: return
        dao.upsertVoicePack(
            existing.copy(
                status = status,
                installDir = installDir ?: existing.installDir,
                installedSizeBytes = installedSizeBytes ?: existing.installedSizeBytes,
                updatedAt = Instant.now(),
            ),
        )
    }

    override suspend fun markInstalled(
        id: String,
        installDir: String,
        installedSizeBytes: Long,
    ) {
        val existing = dao.getVoicePackById(id) ?: return
        dao.upsertVoicePack(
            existing.copy(
                installDir = installDir,
                installedSizeBytes = installedSizeBytes,
                status = VoicePackStatus.READY.storageValue,
                updatedAt = Instant.now(),
            ),
        )
    }

    override fun voicePackRootDir(): File = File(appContext.filesDir, "voice-packs")
}

internal fun parseVoicePackManifest(
    jsonText: String,
    existingById: Map<String, VoicePack>,
    currentActiveId: String?,
    now: Instant,
): List<VoicePack> {
    val root = JSONObject(jsonText)
    val items = root.optJSONArray("voicePacks") ?: JSONArray()
    val voicePacks = mutableListOf<VoicePack>()
    repeat(items.length()) { index ->
        val item = items.optJSONObject(index) ?: return@repeat
        val id = item.optString("id").trim()
        if (id.isBlank()) {
            return@repeat
        }
        val existing = existingById[id]
        voicePacks += VoicePack(
            id = id,
            name = item.optString("name").ifBlank { id },
            locale = item.optString("locale").ifBlank { "en-US" },
            accent = item.optString("accent").ifBlank { "auto" },
            engineType = item.optString("engineType").ifBlank { VoicePackEngineType.SHERPA_ONNX.storageValue },
            version = item.optString("version").ifBlank { "1" },
            downloadUrl = item.optString("downloadUrl").takeIf(String::isNotBlank) ?: existing?.downloadUrl,
            manifestUrl = item.optString("manifestUrl").takeIf(String::isNotBlank) ?: existing?.manifestUrl,
            installDir = existing?.installDir,
            archiveChecksum = item.optString("archiveChecksum").takeIf(String::isNotBlank) ?: existing?.archiveChecksum,
            installedSizeBytes = existing?.installedSizeBytes ?: 0L,
            status = existing?.status ?: VoicePackStatus.NOT_INSTALLED.storageValue,
            isActive = currentActiveId == id,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
    }
    return voicePacks
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
