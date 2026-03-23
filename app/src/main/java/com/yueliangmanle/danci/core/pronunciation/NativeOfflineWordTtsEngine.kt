package com.yueliangmanle.danci.core.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.buildGeneratedContentHash
import com.yueliangmanle.danci.core.data.buildGeneratedNamespace
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackEngineType
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.WordAudioAsset
import java.io.File

data class NativeWordSynthesisResult(
    val asset: WordAudioAsset,
    val outputFile: File,
    val normalizedWord: String,
    val voicePack: VoicePack,
    val cacheHit: Boolean,
)

class NativeOfflineWordTtsEngine(
    context: Context,
    private val voicePackRepository: VoicePackRepository,
    private val wordAudioRepository: WordAudioRepository,
    private val runtimeLoader: (File) -> SherpaOnnxRuntime = { installDir ->
        SherpaOnnxRuntime.Loader(installDir).load()
    },
) {
    private val appContext = context.applicationContext

    suspend fun synthesizeWord(
        word: Word,
        accent: PronunciationAccent,
    ): NativeWordSynthesisResult? {
        val normalizedWord = normalizeWordForPronunciation(word.lemma) ?: return null
        val voicePack = resolveInstalledNativePack(accent) ?: return null
        val resolvedAccent = resolveAccent(accent, voicePack)
        findCachedWord(word, voicePack, resolvedAccent)?.let { return it }
        val installDir = voicePack.installDir?.takeIf(String::isNotBlank)?.let(::File) ?: return null
        if (!installDir.exists() || !installDir.isDirectory) {
            return null
        }

        val stagingFile = buildStagingOutputFile(normalizedWord, resolvedAccent)
        runtimeLoader(installDir).synthesizeWord(normalizedWord, stagingFile)
        val namespace = buildWordNamespace(
            normalizedWord = normalizedWord,
            accent = resolvedAccent,
            voicePack = voicePack,
        )

        val asset = wordAudioRepository.cacheNativeGeneratedAudioWithContext(
            wordId = word.id,
            accent = resolvedAccent,
            normalizedWord = normalizedWord,
            modelFamily = voicePack.modelFamily.orCacheModelFamily(),
            packVersion = voicePack.version.orCachePackVersion(),
            sourceId = voicePack.id,
            presetId = null,
            sourceFile = stagingFile,
            namespace = namespace,
        ) ?: return null

        val outputFile = asset.localPath?.let(::File) ?: return null
        return NativeWordSynthesisResult(
            asset = asset,
            outputFile = outputFile,
            normalizedWord = normalizedWord,
            voicePack = voicePack,
            cacheHit = false,
        )
    }

    suspend fun resolveCacheNamespace(
        accent: PronunciationAccent,
    ): NativeWordCacheNamespace? {
        val voicePack = resolveInstalledNativePack(accent) ?: return null
        val resolvedAccent = resolveAccent(accent, voicePack)
        return NativeWordCacheNamespace(
            accent = resolvedAccent,
            namespace = buildGeneratedNamespace(
                accent = resolvedAccent,
                modelFamily = voicePack.modelFamily.orCacheModelFamily(),
                packVersion = voicePack.version.orCachePackVersion(),
                sourceId = voicePack.id,
            ),
        )
    }

    private suspend fun resolveInstalledNativePack(accent: PronunciationAccent): VoicePack? {
        val activePack = voicePackRepository.getActiveVoicePack(accent)
            ?.takeIf(::isInstalledNativePack)
        if (activePack != null) {
            return activePack
        }

        return voicePackRepository.getAllVoicePacks()
            .asSequence()
            .filter(::isInstalledNativePack)
            .firstOrNull { pack ->
                accent == PronunciationAccent.AUTO ||
                    PronunciationAccent.fromStorageValue(pack.accent) == accent
            }
    }

    private fun isInstalledNativePack(voicePack: VoicePack): Boolean =
        voicePack.status == VoicePackStatus.READY.storageValue &&
            VoicePackEngineType.fromStorageValue(voicePack.engineType) == VoicePackEngineType.SHERPA_ONNX &&
            !voicePack.installDir.isNullOrBlank()

    private fun resolveAccent(
        requestedAccent: PronunciationAccent,
        voicePack: VoicePack,
    ): PronunciationAccent =
        if (requestedAccent != PronunciationAccent.AUTO) {
            requestedAccent
        } else {
            PronunciationAccent.fromStorageValue(voicePack.accent)
        }

    private fun buildStagingOutputFile(
        normalizedWord: String,
        accent: PronunciationAccent,
    ): File {
        val target = File(
            appContext.cacheDir,
            "native-word-synthesis/${accent.storageValue}/$normalizedWord.wav",
        )
        target.parentFile?.mkdirs()
        return target
    }

    private suspend fun findCachedWord(
        word: Word,
        voicePack: VoicePack,
        resolvedAccent: PronunciationAccent,
    ): NativeWordSynthesisResult? {
        val normalizedWord = normalizeWordForPronunciation(word.lemma) ?: word.lemma
        val namespace = buildWordNamespace(
            normalizedWord = normalizedWord,
            accent = resolvedAccent,
            voicePack = voicePack,
        )
        val asset = wordAudioRepository.findNativeGeneratedAssetWithContext(
            wordId = word.id,
            accent = resolvedAccent,
            expectedNamespace = namespace,
            sourceId = voicePack.id,
            presetId = null,
        ) ?: return null
        val outputFile = asset.localPath?.let(::File)
            ?.takeIf { it.exists() }
            ?: return null
        return NativeWordSynthesisResult(
            asset = asset,
            outputFile = outputFile,
            normalizedWord = normalizedWord,
            voicePack = voicePack,
            cacheHit = true,
        )
    }

    private fun buildWordNamespace(
        normalizedWord: String,
        accent: PronunciationAccent,
        voicePack: VoicePack,
    ): String =
        buildGeneratedNamespace(
            accent = accent,
            modelFamily = voicePack.modelFamily.orCacheModelFamily(),
            packVersion = voicePack.version.orCachePackVersion(),
            sourceId = voicePack.id,
            sceneType = "word",
            contentHash = buildGeneratedContentHash(normalizedWord),
        )

    suspend fun markPlayed(asset: WordAudioAsset) {
        wordAudioRepository.markPlayed(asset)
    }
}

data class NativeWordCacheNamespace(
    val accent: PronunciationAccent,
    val namespace: String,
)

private fun String?.orCacheModelFamily(): String = this?.takeIf(String::isNotBlank) ?: "unknown-model"

private fun String?.orCachePackVersion(): String = this?.takeIf(String::isNotBlank) ?: "unknown-version"
