package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.room.withTransaction
import com.yueliangmanle.danci.core.database.buildDanciDatabase
import com.yueliangmanle.danci.core.database.dao.VoicePackDao
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.pronunciation.LicenseManifestVerifier
import com.yueliangmanle.danci.core.pronunciation.NativeVoicePackLicense
import com.yueliangmanle.danci.core.pronunciation.NativeVoicePackManifest
import java.io.File
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

private const val VOICE_PACK_MANIFEST_ASSET_PATH = "pronunciation/voice-pack-manifest.json"
private const val INSTALLED_VOICE_PACK_MANIFEST_FILE = "manifest.json"

private data class BundledCatalogMetadata(
    val voicePacksById: Map<String, VoicePack>,
    val manifestsById: Map<String, NativeVoicePackManifest>,
)

interface VoicePackRepository {
    suspend fun getAllVoicePacks(): List<VoicePack>
    suspend fun getVoicePack(id: String): VoicePack?
    suspend fun getActiveVoicePack(): VoicePack? =
        getAllVoicePacks()
            .asSequence()
            .filter { it.isActive }
            .singleOrNull()

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
            ?.let(::hydrateRuntimeMetadata)

    override suspend fun getActiveVoicePack(): VoicePack? =
        resolveSingleActiveVoicePack(dao.getAllVoicePacks())

    override suspend fun getActiveVoicePack(accent: PronunciationAccent): VoicePack? =
        when (accent) {
            PronunciationAccent.AUTO -> getActiveVoicePack()
            else -> dao.getActiveVoicePackForAccent(accent.storageValue)
                ?.asExternalModel()
                ?.let(::hydrateRuntimeMetadata)
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
        val existingVoicePacks = dao.getAllVoicePacks().map(VoicePackEntity::asExternalModel)
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
        val jsonText = appContext.assets.open(VOICE_PACK_MANIFEST_ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }
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
        val bundledCatalog = loadBundledCatalogMetadata()
        return voicePacks.map { voicePack ->
            val catalogVoicePack = bundledCatalog.voicePacksById[voicePack.id]
            voicePack.hydrateRuntimeMetadata(
                catalogVoicePack = catalogVoicePack,
                catalogManifest = bundledCatalog.manifestsById[voicePack.id],
                installedManifest = loadInstalledRuntimeMetadata(voicePack),
            )
        }
    }

    private fun hydrateRuntimeMetadata(voicePack: VoicePack): VoicePack {
        val bundledCatalog = loadBundledCatalogMetadata()
        val catalogVoicePack = bundledCatalog.voicePacksById[voicePack.id]
        return voicePack.hydrateRuntimeMetadata(
            catalogVoicePack = catalogVoicePack,
            catalogManifest = bundledCatalog.manifestsById[voicePack.id],
            installedManifest = loadInstalledRuntimeMetadata(voicePack),
        )
    }

    private fun loadBundledCatalogMetadata(): BundledCatalogMetadata =
        runCatching {
            val jsonText = appContext.assets.open(VOICE_PACK_MANIFEST_ASSET_PATH)
                .bufferedReader()
                .use { it.readText() }
            val voicePacks = parseVoicePackManifest(
                jsonText = jsonText,
                existingById = emptyMap(),
                currentActiveIdsByAccent = emptyMap(),
                now = Instant.EPOCH,
            )
            val root = JSONObject(jsonText)
            val items = root.optJSONArray("voicePacks") ?: JSONArray()
            val manifestsById = buildMap {
                repeat(items.length()) { index ->
                    val item = items.optJSONObject(index) ?: return@repeat
                    val id = item.optString("id").trim().takeIf(String::isNotEmpty) ?: return@repeat
                    val manifest = NativeVoicePackManifest.fromCatalogItem(item)
                    if (!manifest.isEmpty()) {
                        put(id, manifest)
                    }
                }
            }
            BundledCatalogMetadata(
                voicePacksById = voicePacks.associateBy(VoicePack::id),
                manifestsById = manifestsById,
            )
        }.getOrDefault(BundledCatalogMetadata(emptyMap(), emptyMap()))

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

    private fun resolveSingleActiveVoicePack(
        entities: List<VoicePackEntity>,
    ): VoicePack? =
        entities
            .asSequence()
            .filter { it.isActive }
            .singleOrNull()
            ?.asExternalModel()
            ?.let(::hydrateRuntimeMetadata)
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
        val accent = item.optString("accent").ifBlank { existing?.accent ?: PronunciationAccent.AUTO.storageValue }
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
            checksumsUrl = item.optString("checksumsUrl").takeIf(String::isNotBlank) ?: existing?.checksumsUrl,
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
            licenses = nativeManifest.licenseLabels().ifEmpty { existing?.licenses.orEmpty() },
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
        checksumsUrl = checksumsUrl,
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
        checksumsUrl = checksumsUrl,
        installDir = installDir,
        archiveChecksum = archiveChecksum,
        installedSizeBytes = installedSizeBytes,
        status = status,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun VoicePack.hydrateRuntimeMetadata(
    catalogVoicePack: VoicePack?,
    catalogManifest: NativeVoicePackManifest?,
    installedManifest: NativeVoicePackManifest?,
): VoicePack {
    val merged = listOfNotNull(catalogManifest, installedManifest)
        .fold(toNativeVoicePackManifest() ?: NativeVoicePackManifest()) { current, next ->
            current.merge(next)
        }
    return copy(
        checksumsUrl = catalogVoicePack?.checksumsUrl ?: checksumsUrl,
        engineFamily = merged.engineFamily ?: engineFamily,
        modelFamily = merged.modelFamily ?: modelFamily,
        supportsImportedWords = merged.supportsImportedWords || supportsImportedWords,
        estimatedStorageBytes = merged.estimatedStorageBytes ?: estimatedStorageBytes,
        estimatedRamMb = merged.estimatedRamMb ?: estimatedRamMb,
        licenses = merged.licenseLabels().ifEmpty { licenses },
    )
}

private fun VoicePack.toNativeVoicePackManifest(): NativeVoicePackManifest? {
    val manifest = NativeVoicePackManifest(
        id = id,
        name = name,
        accent = accent,
        locale = locale,
        engineType = engineFamily ?: engineType,
        engineFamily = engineFamily,
        modelFamily = modelFamily,
        modelVersion = version,
        estimatedStorageBytes = estimatedStorageBytes,
        estimatedRamMb = estimatedRamMb,
        licenses = licenses.map { label -> NativeVoicePackLicense(name = label) },
        supportsImportedWords = supportsImportedWords,
    )
    return manifest.takeUnless(NativeVoicePackManifest::isEmpty)
}

private fun NativeVoicePackManifest.merge(other: NativeVoicePackManifest): NativeVoicePackManifest =
    NativeVoicePackManifest(
        id = other.id ?: id,
        name = other.name ?: name,
        accent = other.accent ?: accent,
        locale = other.locale ?: locale,
        engineType = other.engineType ?: engineType,
        engineFamily = other.engineFamily ?: engineFamily,
        modelFamily = other.modelFamily ?: modelFamily,
        modelVersion = other.modelVersion ?: modelVersion,
        packageFormatVersion = other.packageFormatVersion ?: packageFormatVersion,
        entryFiles = other.entryFiles.ifEmpty { entryFiles },
        payloadChecksums = other.payloadChecksums.ifEmpty { payloadChecksums },
        estimatedStorageBytes = other.estimatedStorageBytes ?: estimatedStorageBytes,
        estimatedRamMb = other.estimatedRamMb ?: estimatedRamMb,
        speakerProfile = other.speakerProfile ?: speakerProfile,
        licenses = other.licenses.ifEmpty { licenses },
        supportsImportedWords = supportsImportedWords || other.supportsImportedWords,
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
