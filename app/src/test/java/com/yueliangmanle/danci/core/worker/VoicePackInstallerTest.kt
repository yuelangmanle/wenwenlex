package com.yueliangmanle.danci.core.worker

import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VoicePackInstallerTest {
    @Test
    fun validateInstalledVoicePackRejectsNativeManifestWithoutLicenses() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-us-offline-word-v1",
                      "engineFamily": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "supportsImportedWords": true,
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "entryFiles": ["model.onnx"],
                      "licenses": []
                    }
                """.trimIndent(),
            )
            installDir.resolve("model.onnx").writeText("fake")

            var error: IllegalStateException? = null
            try {
                validateInstalledVoicePack(
                    voicePack = TestVoicePackFactory.voicePack(
                        id = "en-us-offline-word-v1",
                        engineType = "sherpa_onnx",
                        installDir = installDir.absolutePath,
                        status = VoicePackStatus.READY.storageValue,
                    ),
                    installDir = installDir,
                )
                fail("Expected native pack validation to fail when licenses are missing.")
            } catch (expected: IllegalStateException) {
                error = expected
            }

            assertTrue(error?.message.orEmpty().contains("licenses"))
        } finally {
            installDir.deleteRecursively()
        }
    }

    @Test
    fun validateInstalledVoicePackRejectsNativeManifestWithoutEntryFiles() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-us-offline-word-v1",
                      "engineFamily": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "supportsImportedWords": true,
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "licenses": ["Apache-2.0"]
                    }
                """.trimIndent(),
            )

            var error: IllegalStateException? = null
            try {
                validateInstalledVoicePack(
                    voicePack = TestVoicePackFactory.voicePack(
                        id = "en-us-offline-word-v1",
                        engineType = "sherpa_onnx",
                        installDir = installDir.absolutePath,
                        status = VoicePackStatus.READY.storageValue,
                    ),
                    installDir = installDir,
                )
                fail("Expected native pack validation to fail when entryFiles are missing.")
            } catch (expected: IllegalStateException) {
                error = expected
            }

            assertTrue(error?.message.orEmpty().contains("entryFiles"))
        } finally {
            installDir.deleteRecursively()
        }
    }

    @Test
    fun validateInstalledVoicePackAcceptsNativeManifestWithEntryFilesAndLicenses() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-gb-offline-word-v1",
                      "engineFamily": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "supportsImportedWords": true,
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "entryFiles": ["model.onnx", "tokens.txt"],
                      "licenses": ["Apache-2.0"]
                    }
                """.trimIndent(),
            )
            installDir.resolve("model.onnx").writeText("fake")
            installDir.resolve("tokens.txt").writeText("fake")

            validateInstalledVoicePack(
                voicePack = TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    engineType = "sherpa_onnx",
                    installDir = installDir.absolutePath,
                    status = VoicePackStatus.READY.storageValue,
                ),
                installDir = installDir,
            )
        } finally {
            installDir.deleteRecursively()
        }
    }
}
