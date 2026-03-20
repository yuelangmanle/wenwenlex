package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.room.withTransaction
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.VoicePackDao
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.pronunciation.LicenseManifestVerifier
import com.yueliangmanle.danci.core.pronunciation.NativeVoicePackManifest
import java.io.File
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

private const val VOICE_PACK_MANIFEST_ASSET_PATH = "pronunciation/voice-pack-manifest.json"
private const val INSTALLED_VOICE_PACK_MANIFEST_FILE = "manifest.json"

interface VoicePackRepository {
    suspend fun getAllVoicePacks(): List<VoicePack>
    suspend fun getVoicePack(id: String): VoicePack?
    suspend fun getActiveVoicePack(): VoicePack?
    suspend fun getActiveVoicePack(accent: PronunciationAccent): VoicePack? =
        when (accent) {
            PronunciationAccent.AUTO -> getActiveVoicePack()
            else -> getAllVoicePacks().firstOrNull { pack ->
                pack.isActive && PronunciationAccent.fromStorageValue(pack.accent) == accent
            }
        }
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
        hydrateRuntimeMetadata(dao.getAllVoicePacks().map(VoicePackEntity::asExternalModel))

    override suspend fun getVoicePack(id: String): VoicePack? =
        dao.getVoicePackById(id)
            ?.asExternalModel()
            ?.let { hydrateRuntimeMetadata(it) }

    override suspend fun getActiveVoicePack(): VoicePack? =
        dao.getActiveVoicePack()
            ?.asExternalModel()
            ?.let { hydrateRuntimeMetadata(it) }

    override suspend fun getActiveVoicePack(accent: PronunciationAccent): VoicePack? =
        when (accent) {
            PronunciationAccent.AUTO -> getActiveVoicePack()
            else -> dao.getActiveVoicePackForAccent(accent.storageValue)
                ?.asExternalModel()
                ?.let { hydrateRuntimeMetadata(it) }
        }

    override suspend fun activateVoicePack(id: String) {
        val target = dao.getVoicePackById(id) ?: return
        if (target.status != VoicePackStatus.READY.storageValue) {
            return
        }
        database.withTransaction {
            dao.activateVoicePackForAccent(id, target.accent)
        }
    }

    override suspend fun upsertVoicePack(voicePack: VoicePack) {
        dao.upsertVoicePack(voicePack.asEntity())
    }

    override suspend fun removeVoicePack(id: String) {
        val removedPack = dao.getVoicePackById(id) ?: return
        removedPack.installDir?.let(::File)?.takeIf(File::exists)?.deleteRecursively()
        database.withTransaction {
            if (removedPack.isActive) {
                dao.clearActiveVoicePackForAccent(removedPack.accent)
            }
            dao.deleteVoicePackById(id)
        }
    }

    override suspend fun syncManifest(jsonText: String): Int {
        val now = Instant.now()
        val existingVoicePacks = dao.getAllVoicePacks()
            .map(VoicePackEntity::asExternalModel)
        val existingById = existingVoicePacks.associateBy(VoicePack::id)
        val currentActiveIdsByAccent = existingVoicePacks
            .asSequence()
            .filter { it.isActive }
            .associate { it.accent to it.id }
        val voicePacks = parseVoicePackManifest(
            jsonText = jsonText,
            existingById = existingById,
            currentActiveIdsByAccent = currentActiveIdsByAccent,
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

    private fun hydrateRuntimeMetadata(voicePacks: List<VoicePack>): List<VoicePack> {
        if (voicePacks.isEmpty()) {
            return emptyList()
        }
        val catalogMetadataById = loadBundledCatalogRuntimeMetadata()
        return voicePacks.map { voicePack ->
            voicePack.hydrateRuntimeMetadata(
                catalogManifest = catalogMetadataById[voicePack.id],
                installedManifest = loadInstalledRuntimeMetadata(voicePack),
            )
        }
    }

    private fun hydrateRuntimeMetadata(voicePack: VoicePack): VoicePack {
        val catalogMetadataById = loadBundledCatalogRuntimeMetadata()
        return voicePack.hydrateRuntimeMetadata(
            catalogManifest = catalogMetadataById[voicePack.id],
            installedManifest = loadInstalledRuntimeMetadata(voicePack),
        )
    }

    private fun loadBundledCatalogRuntimeMetadata(): Map<String, NativeVoicePackManifest> =
        runCatching {
            val jsonText = appContext.assets.open(VOICE_PACK_MANIFEST_ASSET_PATH)
                .bufferedReader()
                .use { it.readText() }
            parseVoicePackManifest(
                jsonText = jsonText,
                existingById = emptyMap(),
                currentActiveIdsByAccent = emptyMap(),
                now = Instant.EPOCH,
            )
                .mapNotNull { voicePack ->
                    voicePack.toNativeVoicePackManifest()
                        ?.takeUnless(NativeVoicePackManifest::isEmpty)
                        ?.let { manifest -> voicePack.id to manifest }
                }
                .toMap()
        }.getOrDefault(emptyMap())

    private fun loadInstalledRuntimeMetadata(voicePack: VoicePack): NativeVoicePackManifest? {
        val installDir = voicePack.installDir?.trim().takeIf { !it.isNullOrEmpty() } ?: return null
        val manifestFile = File(installDir, INSTALLED_VOICE_PACK_MANIFEST_FILE)
        if (!manifestFile.exists()) {
            return null
        }
        return runCatching {
            NativeVoicePackManifest.fromInstalledManifest(JSONObject(manifestFile.readText()))
                .also(LicenseManifestVerifier::requireValid)
        }.getOrNull()
    }
}

internal fun parseVoicePackManifest(
    jsonText: String,
    existingById: Map<String, VoicePack>,
    currentActiveIdsByAccent: Map<String, String>,
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
        val accent = item.optString("accent").ifBlank { existing?.accent ?: "auto" }
        val nativeManifest = NativeVoicePackManifest.fromCatalogItem(item)
        voicePacks += VoicePack(
            id = id,
            name = item.optString("name").ifBlank { id },
            locale = item.optString("locale").ifBlank { "en-US" },
            accent = accent,
            engineType = item.optString("engineType").ifBlank { VoicePackEngineType.SHERPA_ONNX.storageValue },
            version = item.optString("version").ifBlank { "1" },
            downloadUrl = item.optString("downloadUrl").takeIf(String::isNotBlank) ?: existing?.downloadUrl,
            manifestUrl = item.optString("manifestUrl").takeIf(String::isNotBlank) ?: existing?.manifestUrl,
            installDir = existing?.installDir,
            archiveChecksum = item.optString("archiveChecksum").takeIf(String::isNotBlank) ?: existing?.archiveChecksum,
            installedSizeBytes = existing?.installedSizeBytes ?: 0L,
            status = existing?.status ?: VoicePackStatus.NOT_INSTALLED.storageValue,
            isActive = currentActiveIdsByAccent[accent] == id,
            engineFamily = nativeManifest.engineFamily ?: existing?.engineFamily,
            modelFamily = nativeManifest.modelFamily ?: existing?.modelFamily,
            supportsImportedWords = nativeManifest.supportsImportedWords || (existing?.supportsImportedWords == true),
            estimatedStorageBytes = nativeManifest.estimatedStorageBytes ?: existing?.estimatedStorageBytes,
            estimatedRamMb = nativeManifest.estimatedRamMb ?: existing?.estimatedRamMb,
            licenses = nativeManifest.licenses.ifEmpty { existing?.licenses.orEmpty() },
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
        engineFamily = null,
        modelFamily = null,
        supportsImportedWords = false,
        estimatedStorageBytes = null,
        estimatedRamMb = null,
        licenses = emptyList(),
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

private fun VoicePack.hydrateRuntimeMetadata(
    catalogManifest: NativeVoicePackManifest?,
    installedManifest: NativeVoicePackManifest?,
): VoicePack {
    val merged = listOfNotNull(catalogManifest, installedManifest)
        .fold(toNativeVoicePackManifest() ?: NativeVoicePackManifest()) { current, next ->
            current.merge(next)
        }
    return copy(
        engineFamily = merged.engineFamily,
        modelFamily = merged.modelFamily,
        supportsImportedWords = merged.supportsImportedWords,
        estimatedStorageBytes = merged.estimatedStorageBytes,
        estimatedRamMb = merged.estimatedRamMb,
        licenses = merged.licenses,
    )
}

private fun VoicePack.toNativeVoicePackManifest(): NativeVoicePackManifest? {
    val manifest = NativeVoicePackManifest(
        engineFamily = engineFamily,
        modelFamily = modelFamily,
        supportsImportedWords = supportsImportedWords,
        estimatedStorageBytes = estimatedStorageBytes,
        estimatedRamMb = estimatedRamMb,
        licenses = licenses,
    )
    return manifest.takeUnless(NativeVoicePackManifest::isEmpty)
}

private fun NativeVoicePackManifest.merge(other: NativeVoicePackManifest): NativeVoicePackManifest =
    NativeVoicePackManifest(
        engineFamily = other.engineFamily ?: engineFamily,
        modelFamily = other.modelFamily ?: modelFamily,
        supportsImportedWords = supportsImportedWords || other.supportsImportedWords,
        estimatedStorageBytes = other.estimatedStorageBytes ?: estimatedStorageBytes,
        estimatedRamMb = other.estimatedRamMb ?: estimatedRamMb,
        licenses = other.licenses.ifEmpty { licenses },
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
