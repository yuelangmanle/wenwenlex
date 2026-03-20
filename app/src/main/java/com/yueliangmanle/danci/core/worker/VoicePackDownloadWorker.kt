package com.yueliangmanle.danci.core.worker

import android.content.Context
import android.content.res.AssetManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.buildVoicePackRepository
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.pronunciation.LicenseManifestVerifier
import com.yueliangmanle.danci.core.pronunciation.NativeVoicePackManifest
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val INPUT_VOICE_PACK_ID = "voice_pack_id"
private const val OUTPUT_ERROR_MESSAGE = "error_message"
private const val VOICE_PACK_INSTALL_MANIFEST_FILE = "manifest.json"
private val CHECKSUM_LINE_REGEX = Regex("^([A-Fa-f0-9]{64})\\s+[*]?(\\S.+)$")

const val VOICE_PACK_DOWNLOAD_WORK_PREFIX = "voice_pack_download_"

interface VoicePackDownloadController {
    suspend fun enqueue(voicePackId: String, allowCellular: Boolean)

    suspend fun latestFailureMessage(voicePackId: String): String? = null
}

class VoicePackDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val voicePackId = inputData.getString(INPUT_VOICE_PACK_ID)?.takeIf(String::isNotBlank)
            ?: return Result.failure()
        val repository = buildVoicePackRepository(applicationContext)
        val voicePack = repository.getVoicePack(voicePackId) ?: return Result.failure()

        return try {
            val installer = VoicePackInstaller(
                assetManager = applicationContext.assets,
                cacheDir = File(applicationContext.cacheDir, "voice-pack-downloads"),
                installRootDir = repository.voicePackRootDir(),
            )
            val installResult = installer.install(
                voicePack = voicePack,
                onStatusChange = { status ->
                    repository.updateVoicePackStatus(voicePackId, status)
                },
            )
            repository.markInstalled(
                id = voicePackId,
                installDir = installResult.installDir.absolutePath,
                installedSizeBytes = installResult.installedSizeBytes,
            )
            Result.success()
        } catch (error: Throwable) {
            repository.updateVoicePackStatus(voicePackId, VoicePackStatus.BROKEN.storageValue)
            Result.failure(
                workDataOf(
                    OUTPUT_ERROR_MESSAGE to (error.message ?: "语音包安装失败，请重试。"),
                ),
            )
        }
    }
}

class VoicePackDownloadScheduler(
    context: Context,
    private val repository: VoicePackRepository = buildVoicePackRepository(context.applicationContext),
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
) : VoicePackDownloadController {
    override suspend fun enqueue(voicePackId: String, allowCellular: Boolean) {
        val voicePack = repository.getVoicePack(voicePackId) ?: return
        repository.updateVoicePackStatus(voicePackId, VoicePackStatus.DOWNLOADING.storageValue)
        workManager.enqueueUniqueWork(
            uniqueWorkName(voicePackId),
            ExistingWorkPolicy.REPLACE,
            buildVoicePackDownloadWorkRequest(
                voicePackId = voicePackId,
                allowCellular = allowCellular,
                downloadUrl = voicePack.downloadUrl,
            ),
        )
    }

    override suspend fun latestFailureMessage(voicePackId: String): String? =
        withContext(Dispatchers.IO) {
            workManager.getWorkInfosForUniqueWork(uniqueWorkName(voicePackId))
                .get()
                .asSequence()
                .filter { it.state == WorkInfo.State.FAILED }
                .mapNotNull { workInfo ->
                    workInfo.outputData
                        .getString(OUTPUT_ERROR_MESSAGE)
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)
                }
                .firstOrNull()
        }
}

internal fun buildVoicePackDownloadWorkRequest(
    voicePackId: String,
    allowCellular: Boolean,
    downloadUrl: String?,
): OneTimeWorkRequest {
    val isAssetInstall = downloadUrl.orEmpty().startsWith("asset://")
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(
            if (isAssetInstall) {
                NetworkType.NOT_REQUIRED
            } else if (allowCellular) {
                NetworkType.CONNECTED
            } else {
                NetworkType.UNMETERED
            },
        )
        .build()
    return OneTimeWorkRequestBuilder<VoicePackDownloadWorker>()
        .setInputData(voicePackWorkData(voicePackId))
        .setConstraints(constraints)
        .addTag(uniqueWorkName(voicePackId))
        .build()
}

private fun voicePackWorkData(voicePackId: String): Data =
    workDataOf(INPUT_VOICE_PACK_ID to voicePackId)

private fun uniqueWorkName(voicePackId: String): String =
    "$VOICE_PACK_DOWNLOAD_WORK_PREFIX$voicePackId"

internal data class VoicePackInstallResult(
    val installDir: File,
    val installedSizeBytes: Long,
)

internal class VoicePackInstaller(
    private val assetManager: AssetManager,
    private val cacheDir: File,
    private val installRootDir: File,
) {
    suspend fun install(
        voicePack: VoicePack,
        onStatusChange: suspend (String) -> Unit,
    ): VoicePackInstallResult {
        val downloadUrl = voicePack.downloadUrl?.takeIf(String::isNotBlank)
            ?: error("语音包缺少下载地址。")
        val installDir = File(installRootDir, voicePack.id)
        installDir.deleteRecursively()
        installDir.mkdirs()

        if (downloadUrl.startsWith("asset://")) {
            onStatusChange(VoicePackStatus.INSTALLING.storageValue)
            installFromAssetDirectory(
                assetPath = downloadUrl.removePrefix("asset://"),
                installDir = installDir,
            )
        } else {
            cacheDir.mkdirs()
            val archiveFile = File(cacheDir, resolveArchiveFileName(voicePack))
            onStatusChange(VoicePackStatus.DOWNLOADING.storageValue)
            val checksum = downloadRemoteArchive(downloadUrl, archiveFile)
            val expectedChecksum = resolveExpectedArchiveChecksum(
                voicePack = voicePack,
                archiveFile = archiveFile,
            )
            if (expectedChecksum != null) {
                onStatusChange(VoicePackStatus.VERIFYING.storageValue)
                check(checksum.equals(expectedChecksum, ignoreCase = true)) {
                    "语音包归档 checksum 校验失败。"
                }
            }
            onStatusChange(VoicePackStatus.INSTALLING.storageValue)
            unzipArchive(archiveFile, installDir)
        }

        check(File(installDir, VOICE_PACK_INSTALL_MANIFEST_FILE).exists()) {
            "语音包安装目录缺少 manifest.json。"
        }
        validateInstalledVoicePack(
            voicePack = voicePack,
            installDir = installDir,
        )
        return VoicePackInstallResult(
            installDir = installDir,
            installedSizeBytes = installDir.directorySizeBytes(),
        )
    }

    private fun resolveExpectedArchiveChecksum(
        voicePack: VoicePack,
        archiveFile: File,
    ): String? {
        val checksumsUrl = voicePack.checksumsUrl?.trim().takeIf(String::isNotEmpty)
        if (checksumsUrl != null) {
            val checksumIndex = downloadChecksumIndex(checksumsUrl)
            return checksumIndex[archiveFile.name]
                ?: error("Release checksum 清单中缺少 ${archiveFile.name}")
        }
        return voicePack.archiveChecksum?.trim().takeIf(String::isNotEmpty)
    }

    private fun installFromAssetDirectory(
        assetPath: String,
        installDir: File,
    ) {
        copyAssetPath(
            assetManager = assetManager,
            assetPath = assetPath,
            target = installDir,
        )
    }

    private fun downloadRemoteArchive(
        remoteUrl: String,
        targetFile: File,
    ): String {
        val connection = URL(remoteUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.connect()
            check(connection.responseCode in 200..299) {
                "语音包下载失败：HTTP ${connection.responseCode}"
            }
            val bytes = connection.inputStream.use { it.readBytes() }
            targetFile.outputStream().use { it.write(bytes) }
            return sha256(bytes)
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadChecksumIndex(checksumsUrl: String): Map<String, String> {
        val checksumText = downloadTextAssetOrRemote(checksumsUrl)
        return checksumText.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val match = CHECKSUM_LINE_REGEX.matchEntire(line) ?: return@mapNotNull null
                val checksum = match.groupValues[1].lowercase()
                val filename = match.groupValues[2].trim()
                filename.takeIf(String::isNotEmpty)?.let { it to checksum }
            }
            .toMap()
    }

    private fun downloadTextAssetOrRemote(source: String): String =
        if (source.startsWith("asset://")) {
            assetManager.open(source.removePrefix("asset://"))
                .bufferedReader()
                .use { it.readText() }
        } else {
            val connection = URL(source).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 20_000
                connection.instanceFollowRedirects = true
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "语音包校验清单下载失败：HTTP ${connection.responseCode}"
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

    private fun unzipArchive(
        archiveFile: File,
        installDir: File,
    ) {
        val rootPath = installDir.canonicalFile.toPath()
        ZipInputStream(archiveFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = File(installDir, entry.name)
                val canonicalTarget = target.canonicalFile.toPath()
                check(canonicalTarget.startsWith(rootPath)) {
                    "检测到非法压缩路径。"
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        zip.copyTo(output)
                    }
                }
                zip.closeEntry()
            }
        }
    }
}

private fun copyAssetPath(
    assetManager: AssetManager,
    assetPath: String,
    target: File,
) {
    val children = assetManager.list(assetPath).orEmpty()
    if (children.isEmpty()) {
        target.parentFile?.mkdirs()
        assetManager.open(assetPath).use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return
    }
    target.mkdirs()
    children.forEach { child ->
        copyAssetPath(
            assetManager = assetManager,
            assetPath = "$assetPath/$child",
            target = File(target, child),
        )
    }
}

private fun File.directorySizeBytes(): Long {
    if (!exists()) {
        return 0L
    }
    if (isFile) {
        return length()
    }
    return listFiles().orEmpty().sumOf(File::directorySizeBytes)
}

internal fun validateInstalledVoicePack(
    voicePack: VoicePack,
    installDir: File,
) {
    val manifestFile = File(installDir, VOICE_PACK_INSTALL_MANIFEST_FILE)
    check(manifestFile.exists()) {
        "语音包安装目录缺少 manifest.json。"
    }

    if (VoicePackEngineType.fromStorageValue(voicePack.engineType) != VoicePackEngineType.SHERPA_ONNX) {
        return
    }

    val manifestJson = JSONObject(manifestFile.readText())
    val nativeManifest = NativeVoicePackManifest.fromInstalledManifest(manifestJson)
    check(!nativeManifest.isEmpty()) {
        "原生语音包 manifest 缺少 native 元数据。"
    }
    check(nativeManifest.id == voicePack.id) {
        "语音包 manifest 与目标语音包不匹配。"
    }
    LicenseManifestVerifier.requireValid(nativeManifest)

    val entryFiles = nativeManifest.entryFiles
    check(entryFiles.isNotEmpty()) {
        "原生语音包 manifest 缺少 entryFiles 声明。"
    }
    entryFiles.forEach { relativePath ->
        val file = resolveInstallFile(installDir, relativePath)
        check(file.isFile) {
            "原生语音包缺少核心文件: $relativePath"
        }
    }

    val payloadChecksums = nativeManifest.payloadChecksums
    check(payloadChecksums.isNotEmpty()) {
        "原生语音包 manifest 缺少 payloadChecksums 声明。"
    }
    entryFiles.forEach { relativePath ->
        check(payloadChecksums.containsKey(relativePath)) {
            "原生语音包 manifest 缺少 payload checksum: $relativePath"
        }
    }
    payloadChecksums.forEach { (relativePath, expectedSha256) ->
        val file = resolveInstallFile(installDir, relativePath)
        check(file.isFile) { "缺少 payload 文件: $relativePath" }
        check(sha256(file.readBytes()).equals(expectedSha256, ignoreCase = true)) {
            "payload checksum 校验失败: $relativePath"
        }
    }

    nativeManifest.licenses
        .mapNotNull { license -> license.file?.trim()?.takeIf(String::isNotEmpty) }
        .forEach { relativePath ->
        check(resolveInstallFile(installDir, relativePath).isFile) {
            "原生语音包缺少许可证文件: $relativePath"
        }
    }
}

private fun resolveInstallFile(
    installDir: File,
    relativePath: String,
): File {
    val normalizedPath = relativePath.trim()
    check(normalizedPath.isNotEmpty()) {
        "检测到非法安装路径: $relativePath"
    }
    val rootPath = installDir.canonicalFile.toPath()
    val target = File(installDir, normalizedPath).canonicalFile
    check(target.toPath().startsWith(rootPath)) {
        "检测到非法安装路径: $relativePath"
    }
    return target
}

private fun resolveArchiveFileName(voicePack: VoicePack): String =
    voicePack.downloadUrl
        ?.takeIf(String::isNotBlank)
        ?.let { downloadUrl ->
            runCatching { URL(downloadUrl).path.substringAfterLast('/').trim() }.getOrNull()
        }
        ?.takeIf(String::isNotEmpty)
        ?: "${voicePack.id}.zip"

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

fun buildVoicePackDownloadController(context: Context): VoicePackDownloadController =
    VoicePackDownloadScheduler(context.applicationContext)
