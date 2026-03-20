package com.yueliangmanle.danci.core.pronunciation

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SherpaOnnxRuntimeTest {
    @Test
    fun runtimeLoaderRejectsMissingNativeArtifactDirectory() {
        val loader = SherpaOnnxRuntime.Loader(File("/tmp/missing-sherpa-runtime"))
        var error: IllegalStateException? = null
        try {
            loader.load()
            fail("Expected loader.load() to reject a missing runtime directory.")
        } catch (expected: IllegalStateException) {
            error = expected
        }

        assertTrue(error?.message.orEmpty().contains("runtime artifact"))
    }

    @Test
    fun synthesizeWordRejectsBlankInputBeforeInvokingRuntimeBridge() {
        val runtimeDir = createRuntimeDir(
            packageFormat = RuntimeFixturePackageFormat.ROOT,
            includeVoices = true,
            includeDataDir = true,
        )
        val bridge = FakeSherpaBridge()
        val runtime = RealSherpaOnnxRuntime(runtimeDir, bridge)
        val outputFile = File(runtimeDir, "out/blank.wav")

        var error: IllegalStateException? = null
        try {
            runtime.synthesizeWord("   ", outputFile)
            fail("Expected blank synthesis text to be rejected.")
        } catch (expected: IllegalStateException) {
            error = expected
        }

        assertTrue(error?.message.orEmpty().contains("cannot be blank"))
        assertTrue(bridge.requests.isEmpty())
    }

    @Test
    fun synthesizeWordRejectsIncompleteKokoroPayload() {
        val runtimeDir = createRuntimeDir(
            packageFormat = RuntimeFixturePackageFormat.ROOT,
            includeVoices = false,
            includeDataDir = true,
        )
        val bridge = FakeSherpaBridge()
        val runtime = RealSherpaOnnxRuntime(runtimeDir, bridge)
        val outputFile = File(runtimeDir, "out/hello.wav")

        var error: IllegalStateException? = null
        try {
            runtime.synthesizeWord("hello", outputFile)
            fail("Expected missing Kokoro payload files to be rejected.")
        } catch (expected: IllegalStateException) {
            error = expected
        }

        assertTrue(error?.message.orEmpty().contains("voices.bin"))
        assertTrue(bridge.requests.isEmpty())
    }

    @Test
    fun synthesizeWordWritesNonEmptyWavFile() {
        val runtimeDir = createRuntimeDir(
            packageFormat = RuntimeFixturePackageFormat.MODEL_SUBDIRECTORY,
            includeVoices = true,
            includeDataDir = true,
        )
        val bridge = FakeSherpaBridge()
        val runtime = RealSherpaOnnxRuntime(runtimeDir, bridge)
        val outputFile = File(runtimeDir, "out/hello.wav")

        runtime.synthesizeWord("hello", outputFile)

        assertTrue(bridge.requests.single().text == "hello")
        assertTrue(bridge.requests.single().layout.kind == SherpaModelKind.KOKORO)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 44L)
    }
}

private class FakeSherpaBridge : SherpaRuntimeBridge {
    data class Request(
        val text: String,
        val outputFile: File,
        val layout: SherpaModelLayout,
    )

    val requests = mutableListOf<Request>()

    override fun synthesize(
        text: String,
        outputFile: File,
        layout: SherpaModelLayout,
    ) {
        requests += Request(text = text, outputFile = outputFile, layout = layout)
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(ByteArray(64) { 0x01 })
    }
}

private enum class RuntimeFixturePackageFormat {
    ROOT,
    MODEL_SUBDIRECTORY,
}

private fun createRuntimeDir(
    packageFormat: RuntimeFixturePackageFormat,
    includeVoices: Boolean,
    includeDataDir: Boolean,
): File {
    val root = createTempDirectory("sherpa-runtime-test").toFile()
    val payloadRoot = when (packageFormat) {
        RuntimeFixturePackageFormat.ROOT -> root
        RuntimeFixturePackageFormat.MODEL_SUBDIRECTORY -> File(root, "model").apply { mkdirs() }
    }

    File(root, "manifest.json").writeText(
        """
        {
          "id": "fixture-pack",
          "name": "Fixture Pack",
          "accent": "uk",
          "locale": "en-GB",
          "engineType": "sherpa_onnx",
          "modelFamily": "kokoro",
          "modelVersion": "1.4.0",
          "packageFormatVersion": 2,
          "entryFiles": [
            "${if (packageFormat == RuntimeFixturePackageFormat.MODEL_SUBDIRECTORY) "model/" else ""}model.onnx",
            "${if (packageFormat == RuntimeFixturePackageFormat.MODEL_SUBDIRECTORY) "model/" else ""}voices.bin",
            "${if (packageFormat == RuntimeFixturePackageFormat.MODEL_SUBDIRECTORY) "model/" else ""}tokens.txt"
          ]
        }
        """.trimIndent(),
    )

    File(payloadRoot, "model.onnx").writeText("fixture-model")
    File(payloadRoot, "tokens.txt").writeText("a 1")
    if (includeVoices) {
        File(payloadRoot, "voices.bin").writeText("fixture-voices")
    }
    if (includeDataDir) {
        File(payloadRoot, "espeak-ng-data").mkdirs()
        File(payloadRoot, "espeak-ng-data/placeholder.txt").writeText("fixture-data")
    }
    return root
}
