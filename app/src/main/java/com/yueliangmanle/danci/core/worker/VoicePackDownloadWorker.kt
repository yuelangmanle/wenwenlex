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
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val INPUT_VOICE_PACK_ID = "voice_pack_id"
private const val OUTPUT_ERROR_MESSAGE = "error_message"
private const val OUTPUT_ERROR_CODE = "error_code"
private const val VOICE_PACK_INSTALL_MANIFEST_FILE = "manifest.json"

const val VOICE_PACK_DOWNLOAD_WORK_PREFIX = "voice_pack_download_"

interface VoicePackDownloadController {
    suspend fun enqueue(voicePackId: String, allowCellular: Boolean)

    suspend fun cancel(voicePackId: String) = Unit

    suspend fun retryWithMirror(
        voicePackId: String,
        downloadUrl: String,
        allowCellular: Boolean,
    ) = Unit

    suspend fun latestFailureMessage(voicePackId: String): String? = null

    suspend fun latestFailureCode(voicePackId: String): String? = null
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
                archiveDownloader = VoicePackArchiveDownloader(
                    shouldCancel = { isStopped },
                ),
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
        } catch (error: VoicePackDownloadCancelledException) {
            repository.updateVoicePackStatus(voicePackId, VoicePackStatus.NOT_INSTALLED.storageValue)
            Result.success()
        } catch (error: VoicePackDownloadFailureException) {
            repository.updateVoicePackStatus(voicePackId, VoicePackStatus.BROKEN.storageValue)
            Result.failure(
                workDataOf(
                    OUTPUT_ERROR_CODE to error.failureCode,
                    OUTPUT_ERROR_MESSAGE to error.message,
                ),
            )
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

    override suspend fun cancel(voicePackId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(voicePackId))
        repository.updateVoicePackStatus(voicePackId, VoicePackStatus.NOT_INSTALLED.storageValue)
    }

    override suspend fun retryWithMirror(
        voicePackId: String,
        downloadUrl: String,
        allowCellular: Boolean,
    ) {
        val voicePack = repository.getVoicePack(voicePackId) ?: return
        repository.upsertVoicePack(
            voicePack.copy(
                downloadUrl = downloadUrl,
                status = VoicePackStatus.NOT_INSTALLED.storageValue,
            ),
        )
        enqueue(
            voicePackId = voicePackId,
            allowCellular = allowCellular,
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

    override suspend fun latestFailureCode(voicePackId: String): String? =
        withContext(Dispatchers.IO) {
            workManager.getWorkInfosForUniqueWork(uniqueWorkName(voicePackId))
                .get()
                .asSequence()
                .filter { it.state == WorkInfo.State.FAILED }
                .mapNotNull { workInfo ->
                    workInfo.outputData
                        .getString(OUTPUT_ERROR_CODE)
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
    private val archiveDownloader: VoicePackArchiveDownloader = VoicePackArchiveDownloader(),
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
            val archiveFile = File(cacheDir, "${voicePack.id}.zip")
            onStatusChange(VoicePackStatus.DOWNLOADING.storageValue)
            val downloadResult = archiveDownloader.download(
                urls = voicePack.downloadUrls.ifEmpty { listOfNotNull(downloadUrl) },
                targetFile = archiveFile,
            )
            if (downloadResult.cancelled) {
                throw VoicePackDownloadCancelledException()
            }
            if (!downloadResult.success) {
                throw VoicePackDownloadFailureException(
                    failureCode = downloadResult.failureCode ?: "source_io_error",
                    message = downloadResult.failureMessage ?: "语音包下载失败，请重试。",
                )
            }
            val checksum = downloadResult.checksum ?: error("语音包下载后校验信息缺失。")
            val expectedChecksum = voicePack.archiveChecksum?.takeIf(String::isNotBlank)
            if (expectedChecksum != null) {
                onStatusChange(VoicePackStatus.VERIFYING.storageValue)
                check(checksum.equals(expectedChecksum, ignoreCase = true)) {
                    "语音包校验失败。"
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

internal class VoicePackDownloadCancelledException : IllegalStateException("语音包下载已取消。")

internal class VoicePackDownloadFailureException(
    val failureCode: String,
    message: String,
) : IllegalStateException(message)

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
    LicenseManifestVerifier.requireValid(nativeManifest)

    val entryFiles = manifestJson.optStringList("entryFiles")
    check(entryFiles.isNotEmpty()) {
        "原生语音包 manifest 缺少 entryFiles 声明。"
    }
    entryFiles.forEach { relativePath ->
        check(File(installDir, relativePath).exists()) {
            "原生语音包缺少核心文件: $relativePath"
        }
    }

    manifestJson.optReferencedLicenseFiles().forEach { relativePath ->
        check(File(installDir, relativePath).exists()) {
            "原生语音包缺少许可证文件: $relativePath"
        }
    }
}

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

private fun JSONObject.optReferencedLicenseFiles(): List<String> {
    val nativeBlock = optJSONObject("native") ?: optJSONObject("runtime") ?: this
    val licenses = nativeBlock.optJSONArray("licenses") ?: return emptyList()
    return buildList {
        repeat(licenses.length()) { index ->
            val licenseObject = licenses.optJSONObject(index) ?: return@repeat
            licenseObject.optString("file")
                .trim()
                .takeIf(String::isNotEmpty)
                ?.let(::add)
        }
    }
}

fun buildVoicePackDownloadController(context: Context): VoicePackDownloadController =
    VoicePackDownloadScheduler(context.applicationContext)
