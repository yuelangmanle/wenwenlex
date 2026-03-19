package com.yueliangmanle.danci.core.pronunciation

import android.content.Context
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
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
        val installDir = voicePack.installDir?.takeIf(String::isNotBlank)?.let(::File) ?: return null
        if (!installDir.exists() || !installDir.isDirectory) {
            return null
        }

        val stagingFile = buildStagingOutputFile(normalizedWord, resolvedAccent)
        runtimeLoader(installDir).synthesizeWord(normalizedWord, stagingFile)

        val asset = wordAudioRepository.cacheNativeGeneratedAudio(
            wordId = word.id,
            accent = resolvedAccent,
            normalizedWord = normalizedWord,
            sourceFile = stagingFile,
        ) ?: return null

        val outputFile = asset.localPath?.let(::File) ?: return null
        return NativeWordSynthesisResult(
            asset = asset,
            outputFile = outputFile,
            normalizedWord = normalizedWord,
            voicePack = voicePack,
        )
    }

    private suspend fun resolveInstalledNativePack(accent: PronunciationAccent): VoicePack? {
        val activePack = voicePackRepository.getActiveVoicePack()
            ?.takeIf(::isInstalledNativePack)
            ?.takeIf { accent == PronunciationAccent.AUTO || resolveAccent(accent, it) == accent }
        if (activePack != null) {
            return activePack
        }

        return voicePackRepository.getAllVoicePacks()
            .asSequence()
            .filter(::isInstalledNativePack)
            .firstOrNull { pack ->
                accent == PronunciationAccent.AUTO || resolveAccent(accent, pack) == accent
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
}
