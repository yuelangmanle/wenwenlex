package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.room.withTransaction
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.VoicePackDao
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
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
                downloadUrl = existing.downloadUrl,
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
        val catalogVoicePacksById = loadBundledCatalogVoicePacks()
        val catalogMetadataById = loadBundledCatalogRuntimeMetadata()
        return voicePacks.map { voicePack ->
            voicePack.withCatalogDownloadSources(catalogVoicePacksById[voicePack.id]).hydrateRuntimeMetadata(
                catalogManifest = catalogMetadataById[voicePack.id],
                installedManifest = loadInstalledRuntimeMetadata(voicePack),
            )
        }
    }

    private fun hydrateRuntimeMetadata(voicePack: VoicePack): VoicePack {
        val catalogVoicePack = loadBundledCatalogVoicePacks()[voicePack.id]
        val catalogMetadataById = loadBundledCatalogRuntimeMetadata()
        return voicePack.withCatalogDownloadSources(catalogVoicePack).hydrateRuntimeMetadata(
            catalogManifest = catalogMetadataById[voicePack.id],
            installedManifest = loadInstalledRuntimeMetadata(voicePack),
        )
    }

    private fun loadBundledCatalogVoicePacks(): Map<String, VoicePack> =
        runCatching {
            val jsonText = appContext.assets.open(VOICE_PACK_MANIFEST_ASSET_PATH)
                .bufferedReader()
                .use { it.readText() }
            parseVoicePackManifest(
                jsonText = jsonText,
                existingById = emptyMap(),
                currentActiveId = null,
                now = Instant.EPOCH,
            ).associateBy(VoicePack::id)
        }.getOrDefault(emptyMap())

    private fun loadBundledCatalogRuntimeMetadata(): Map<String, NativeVoicePackManifest> =
        runCatching {
            val jsonText = appContext.assets.open(VOICE_PACK_MANIFEST_ASSET_PATH)
                .bufferedReader()
                .use { it.readText() }
            parseVoicePackManifest(
                jsonText = jsonText,
                existingById = emptyMap(),
                currentActiveId = null,
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
        val nativeManifest = NativeVoicePackManifest.fromCatalogItem(item)
        val manifestDownloadUrls = item.optStringList("downloadUrls")
            .ifEmpty {
                listOfNotNull(item.optString("downloadUrl").takeIf(String::isNotBlank))
            }
        val mergedDownloadUrls = (existing?.downloadUrls.orEmpty() + manifestDownloadUrls)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        val resolvedDownloadUrl = existing?.downloadUrl
            ?.takeIf { mergedDownloadUrls.contains(it) }
            ?: manifestDownloadUrls.firstOrNull()
            ?: existing?.downloadUrl
        voicePacks += VoicePack(
            id = id,
            name = item.optString("name").ifBlank { id },
            locale = item.optString("locale").ifBlank { "en-US" },
            accent = item.optString("accent").ifBlank { "auto" },
            engineType = item.optString("engineType").ifBlank { VoicePackEngineType.SHERPA_ONNX.storageValue },
            version = item.optString("version").ifBlank { "1" },
            downloadUrl = resolvedDownloadUrl,
            downloadUrls = mergedDownloadUrls.ifEmpty { listOfNotNull(resolvedDownloadUrl) },
            manifestUrl = item.optString("manifestUrl").takeIf(String::isNotBlank) ?: existing?.manifestUrl,
            installDir = existing?.installDir,
            archiveChecksum = item.optString("archiveChecksum").takeIf(String::isNotBlank) ?: existing?.archiveChecksum,
            installedSizeBytes = existing?.installedSizeBytes ?: 0L,
            status = existing?.status ?: VoicePackStatus.NOT_INSTALLED.storageValue,
            isActive = currentActiveId == id,
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
        downloadUrls = listOfNotNull(downloadUrl),
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

private fun VoicePack.withCatalogDownloadSources(
    catalogVoicePack: VoicePack?,
): VoicePack {
    val mergedDownloadUrls = (
        listOfNotNull(downloadUrl) +
            downloadUrls +
            catalogVoicePack?.downloadUrls.orEmpty() +
            listOfNotNull(catalogVoicePack?.downloadUrl)
        )
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
    val resolvedDownloadUrl = downloadUrl
        ?.takeIf { mergedDownloadUrls.contains(it) }
        ?: catalogVoicePack?.downloadUrl
        ?: mergedDownloadUrls.firstOrNull()
    return copy(
        downloadUrl = resolvedDownloadUrl,
        downloadUrls = mergedDownloadUrls,
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

private fun JSONObject.optStringList(key: String): List<String> {
    val items = optJSONArray(key) ?: return emptyList()
    return buildList {
        repeat(items.length()) { index ->
            items.optString(index)
                .trim()
                .takeIf(String::isNotEmpty)
                ?.let(::add)
        }
    }
}

fun buildVoicePackRepository(context: Context): VoicePackRepository {
    val appContext = context.applicationContext
    val database = buildDanciDatabase(appContext)
    return RoomVoicePackRepository(
        appContext = appContext,
        dao = database.voicePackDao(),
        database = database,
    )
}
