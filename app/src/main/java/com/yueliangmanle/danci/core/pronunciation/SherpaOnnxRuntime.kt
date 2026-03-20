package com.yueliangmanle.danci.core.pronunciation

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import org.json.JSONObject

interface SherpaOnnxRuntime {
    fun synthesizeWord(
        text: String,
        outputFile: File,
    )

    class Loader(
        private val runtimeDir: File,
    ) {
        fun load(): SherpaOnnxRuntime {
            check(runtimeDir.exists() && runtimeDir.isDirectory) {
                "Missing Sherpa runtime artifact directory: ${runtimeDir.absolutePath}"
            }
            return RealSherpaOnnxRuntime(runtimeDir)
        }
    }
}

internal class RealSherpaOnnxRuntime(
    private val runtimeDir: File,
    private val bridge: SherpaRuntimeBridge = NativeSherpaRuntimeBridge(),
) : SherpaOnnxRuntime {
    override fun synthesizeWord(
        text: String,
        outputFile: File,
    ) {
        val normalizedText = text.trim()
        check(normalizedText.isNotEmpty()) { "Sherpa ONNX synthesis text cannot be blank." }
        check(runtimeDir.exists() && runtimeDir.isDirectory) {
            "Missing Sherpa runtime artifact directory: ${runtimeDir.absolutePath}"
        }
        outputFile.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) {
                "Unable to create synthesized audio directory: ${parent.absolutePath}"
            }
        }

        val layout = SherpaModelLayout.detect(runtimeDir)
        bridge.synthesize(
            text = normalizedText,
            outputFile = outputFile,
            layout = layout,
        )
        check(outputFile.exists() && outputFile.length() > 44L) {
            "Sherpa ONNX synthesis produced an empty wav file: ${outputFile.absolutePath}"
        }
    }
}

internal interface SherpaRuntimeBridge {
    fun synthesize(
        text: String,
        outputFile: File,
        layout: SherpaModelLayout,
    )
}

internal class NativeSherpaRuntimeBridge : SherpaRuntimeBridge {
    override fun synthesize(
        text: String,
        outputFile: File,
        layout: SherpaModelLayout,
    ) {
        val tts = try {
            OfflineTts(buildConfig(layout))
        } catch (error: UnsatisfiedLinkError) {
            throw IllegalStateException("Sherpa ONNX JNI runtime 未就绪，请先同步 Android 原生库。", error)
        } catch (error: Throwable) {
            throw IllegalStateException("Sherpa ONNX runtime 初始化失败：${error.message}", error)
        }

        try {
            val audio = tts.generate(text)
            check(audio.samples.isNotEmpty()) { "Sherpa ONNX runtime returned empty audio samples." }
            check(audio.save(outputFile.absolutePath)) {
                "Sherpa ONNX runtime failed to save wav file: ${outputFile.absolutePath}"
            }
        } finally {
            tts.release()
        }
    }

    private fun buildConfig(layout: SherpaModelLayout): OfflineTtsConfig {
        val modelBuilder = OfflineTtsModelConfig.builder()
            .setNumThreads(layout.numThreads)
            .setDebug(false)
            .setProvider("cpu")

        when (layout.kind) {
            SherpaModelKind.KOKORO -> {
                val builder = OfflineTtsKokoroModelConfig.builder()
                    .setModel(layout.modelFile.absolutePath)
                    .setVoices(requireNotNull(layout.voicesFile).absolutePath)
                    .setTokens(layout.tokensFile.absolutePath)
                    .setDataDir(requireNotNull(layout.dataDir).absolutePath)
                layout.lexicon?.takeIf(String::isNotBlank)?.let(builder::setLexicon)
                layout.language?.takeIf(String::isNotBlank)?.let(builder::setLang)
                modelBuilder.setKokoro(builder.build())
            }
            SherpaModelKind.KITTEN -> {
                val builder = OfflineTtsKittenModelConfig.builder()
                    .setModel(layout.modelFile.absolutePath)
                    .setVoices(requireNotNull(layout.voicesFile).absolutePath)
                    .setTokens(layout.tokensFile.absolutePath)
                    .setDataDir(requireNotNull(layout.dataDir).absolutePath)
                modelBuilder.setKitten(builder.build())
            }
            SherpaModelKind.VITS -> {
                val builder = OfflineTtsVitsModelConfig.builder()
                    .setModel(layout.modelFile.absolutePath)
                    .setTokens(layout.tokensFile.absolutePath)
                layout.dataDir?.let { builder.setDataDir(it.absolutePath) }
                layout.lexicon?.takeIf(String::isNotBlank)?.let(builder::setLexicon)
                modelBuilder.setVits(builder.build())
            }
        }

        return OfflineTtsConfig.builder()
            .setModel(modelBuilder.build())
            .build()
    }
}

internal enum class SherpaModelKind {
    KOKORO,
    KITTEN,
    VITS,
}

internal data class SherpaModelLayout(
    val kind: SherpaModelKind,
    val modelFile: File,
    val tokensFile: File,
    val voicesFile: File?,
    val dataDir: File?,
    val lexicon: String?,
    val locale: String?,
    val numThreads: Int,
) {
    val language: String?
        get() = locale.toSherpaLanguage()

    companion object {
        fun detect(runtimeDir: File): SherpaModelLayout {
            val manifest = runtimeDir.readInstalledManifest()
            val entryFiles = manifest?.entryFiles.orEmpty()
            val candidateRoots = buildCandidateRoots(runtimeDir, entryFiles)

            val resolvedModelFile = requireNotNull(
                resolvePayloadFile(
                runtimeDir = runtimeDir,
                entryFiles = entryFiles,
                candidateRoots = candidateRoots,
                suffixCandidates = listOf("model.onnx", "model.fp16.onnx"),
                fileNameHints = listOf("model.onnx", "model.fp16.onnx"),
            ) ?: candidateRoots
                .asSequence()
                .flatMap { root -> root.listFiles().orEmpty().asSequence() }
                .firstOrNull { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
            ) {
                "Missing Sherpa model payload under ${runtimeDir.absolutePath}"
            }

            val resolvedTokensFile = requireNotNull(
                resolvePayloadFile(
                runtimeDir = runtimeDir,
                entryFiles = entryFiles,
                candidateRoots = candidateRoots,
                suffixCandidates = listOf("tokens.txt"),
                fileNameHints = listOf("tokens.txt"),
            ),
            ) {
                "Missing Sherpa tokens.txt payload under ${runtimeDir.absolutePath}"
            }

            val voicesFile = resolvePayloadFile(
                runtimeDir = runtimeDir,
                entryFiles = entryFiles,
                candidateRoots = candidateRoots,
                suffixCandidates = listOf("voices.bin"),
                fileNameHints = listOf("voices.bin"),
            )
            val dataDir = resolvePayloadDirectory(
                runtimeDir = runtimeDir,
                candidateRoots = candidateRoots,
                dirNameHints = listOf("espeak-ng-data"),
            )
            val lexicon = candidateRoots
                .flatMap { root ->
                    root.listFiles().orEmpty().asList()
                }
                .filter { file ->
                    file.isFile &&
                        file.name.startsWith("lexicon", ignoreCase = true) &&
                        file.extension.equals("txt", ignoreCase = true)
                }
                .distinctBy(File::absolutePath)
                .joinToString(",") { it.absolutePath }
                .ifBlank { null }

            val kind = detectModelKind(
                manifest = manifest,
                modelFile = resolvedModelFile,
                voicesFile = voicesFile,
            )
            if (kind == SherpaModelKind.KOKORO || kind == SherpaModelKind.KITTEN) {
                check(voicesFile != null) {
                    "Sherpa ${kind.name.lowercase()} runtime requires voices.bin under ${runtimeDir.absolutePath}"
                }
                check(dataDir != null && dataDir.isDirectory) {
                    "Sherpa ${kind.name.lowercase()} runtime requires espeak-ng-data under ${runtimeDir.absolutePath}"
                }
            }

            return SherpaModelLayout(
                kind = kind,
                modelFile = resolvedModelFile,
                tokensFile = resolvedTokensFile,
                voicesFile = voicesFile,
                dataDir = dataDir,
                lexicon = lexicon,
                locale = manifest?.locale,
                numThreads = if (kind == SherpaModelKind.VITS) 2 else 4,
            )
        }

        private fun detectModelKind(
            manifest: NativeVoicePackManifest?,
            modelFile: File,
            voicesFile: File?,
        ): SherpaModelKind {
            val family = manifest?.modelFamily.orEmpty().lowercase()
            return when {
                "kitten" in family || modelFile.name.contains("kitten", ignoreCase = true) ->
                    SherpaModelKind.KITTEN
                "kokoro" in family || voicesFile != null ->
                    SherpaModelKind.KOKORO
                else ->
                    SherpaModelKind.VITS
            }
        }

        private fun buildCandidateRoots(
            runtimeDir: File,
            entryFiles: List<String>,
        ): List<File> =
            buildList {
                add(runtimeDir)
                File(runtimeDir, "model").takeIf { it.exists() && it.isDirectory }?.let(::add)
                entryFiles.mapNotNullTo(this) { relativePath ->
                    relativePath
                        .replace('\\', '/')
                        .substringBeforeLast('/', missingDelimiterValue = "")
                        .takeIf(String::isNotBlank)
                        ?.let { File(runtimeDir, it) }
                        ?.takeIf { it.exists() && it.isDirectory }
                }
            }.distinctBy(File::absolutePath)

        private fun resolvePayloadFile(
            runtimeDir: File,
            entryFiles: List<String>,
            candidateRoots: List<File>,
            suffixCandidates: List<String>,
            fileNameHints: List<String>,
        ): File? {
            entryFiles.forEach { relativePath ->
                val normalized = relativePath.replace('\\', '/')
                if (suffixCandidates.any { normalized.endsWith(it, ignoreCase = true) }) {
                    File(runtimeDir, normalized).takeIf(File::isFile)?.let { return it }
                }
            }
            candidateRoots.forEach { root ->
                fileNameHints.forEach { name ->
                    File(root, name).takeIf(File::isFile)?.let { return it }
                }
            }
            return null
        }

        private fun resolvePayloadDirectory(
            runtimeDir: File,
            candidateRoots: List<File>,
            dirNameHints: List<String>,
        ): File? {
            dirNameHints.forEach { directoryName ->
                File(runtimeDir, directoryName).takeIf(File::isDirectory)?.let { return it }
            }
            candidateRoots.forEach { root ->
                dirNameHints.forEach { directoryName ->
                    File(root, directoryName).takeIf(File::isDirectory)?.let { return it }
                }
            }
            return null
        }
    }
}

private fun File.readInstalledManifest(): NativeVoicePackManifest? {
    val manifestFile = File(this, "manifest.json")
    if (!manifestFile.isFile) {
        return null
    }
    return runCatching {
        NativeVoicePackManifest.fromInstalledManifest(
            JSONObject(manifestFile.readText()),
        )
    }.getOrNull()
}

private fun String?.toSherpaLanguage(): String? {
    val normalized = this?.trim()?.lowercase().orEmpty()
    return when {
        normalized.startsWith("en") -> "eng"
        normalized.startsWith("zh") -> "zho"
        normalized.startsWith("de") -> "deu"
        else -> null
    }
}
