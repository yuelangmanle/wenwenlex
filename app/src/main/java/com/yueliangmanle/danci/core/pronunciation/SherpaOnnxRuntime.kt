package com.yueliangmanle.danci.core.pronunciation

import java.io.File

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
) : SherpaOnnxRuntime {
    override fun synthesizeWord(
        text: String,
        outputFile: File,
    ) {
        check(text.isNotBlank()) { "Sherpa ONNX synthesis text cannot be blank." }
        check(runtimeDir.exists() && runtimeDir.isDirectory) {
            "Missing Sherpa runtime artifact directory: ${runtimeDir.absolutePath}"
        }
        outputFile.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) {
                "Unable to create synthesized audio directory: ${parent.absolutePath}"
            }
        }
        error("Sherpa ONNX runtime bridge is scaffolded but not wired to model execution yet.")
    }
}
