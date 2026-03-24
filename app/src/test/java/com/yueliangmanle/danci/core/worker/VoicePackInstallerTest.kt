package com.yueliangmanle.danci.core.worker

import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.nio.file.Files
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VoicePackInstallerTest {
    @Test
    fun archiveDownloaderResumesFromExistingPartFileWhenRangeSupported() = runTest {
        val tempDir = Files.createTempDirectory("voice-pack-download-test").toFile()
        try {
            val targetFile = tempDir.resolve("pack.zip")
            val partFile = tempDir.resolve("pack.zip.part")
            partFile.writeText("abc")
            val requests = mutableListOf<ArchiveDownloadRequest>()
            val downloader = VoicePackArchiveDownloader(
                fetch = { request ->
                    requests += request
                    ArchiveDownloadResponse(
                        responseCode = 206,
                        body = "def".toByteArray(),
                    )
                },
            )

            val result = downloader.download(
                urls = listOf("https://example.com/pack.zip"),
                targetFile = targetFile,
            )

            assertTrue(result.success)
            assertTrue(result.resumed)
            assertEquals(3L, requests.single().rangeStart)
            assertEquals("abcdef", targetFile.readText())
            assertFalse(partFile.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun archiveDownloaderSuggestsMirrorWhenPrimarySourceTimesOut() = runTest {
        val tempDir = Files.createTempDirectory("voice-pack-download-test").toFile()
        try {
            val targetFile = tempDir.resolve("pack.zip")
            val downloader = VoicePackArchiveDownloader(
                fetch = {
                    throw SocketTimeoutException("timeout")
                },
            )

            val result = downloader.download(
                urls = listOf(
                    "https://primary.example.com/pack.zip",
                    "https://mirror.example.com/pack.zip",
                ),
                targetFile = targetFile,
            )

            assertFalse(result.success)
            assertEquals("source_timeout", result.failureCode)
            assertEquals("https://mirror.example.com/pack.zip", result.nextSuggestedUrl)
        } finally {
            tempDir.deleteRecursively()
        }
    }

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
    fun validateInstalledVoicePackRejectsMissingReferencedLicenseFile() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-us-offline-word-v1",
                      "entryFiles": ["model/model.onnx", "model/tokens.txt"],
                      "native": {
                        "engineFamily": "native_neural_tts",
                        "modelFamily": "sherpa_onnx_scaffold",
                        "supportsImportedWords": true,
                        "licenses": [
                          {
                            "name": "Distribution scaffold notice",
                            "file": "licenses/DISTRIBUTION-NOTICE.txt"
                          }
                        ]
                      }
                    }
                """.trimIndent(),
            )
            installDir.resolve("model").mkdirs()
            installDir.resolve("model/model.onnx").writeText("fake")
            installDir.resolve("model/tokens.txt").writeText("fake")

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
                fail("Expected native pack validation to fail when referenced license file is missing.")
            } catch (expected: IllegalStateException) {
                error = expected
            }

            assertTrue(error?.message.orEmpty().contains("licenses/DISTRIBUTION-NOTICE.txt"))
        } finally {
            installDir.deleteRecursively()
        }
    }

    @Test
    fun validateInstalledVoicePackAcceptsPackagedNativeReleaseStructure() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-gb-offline-word-v1",
                      "entryFiles": ["model/model.onnx", "model/tokens.txt"],
                      "native": {
                        "engineFamily": "native_neural_tts",
                        "modelFamily": "sherpa_onnx_scaffold",
                        "supportsImportedWords": true,
                        "licenses": [
                          {
                            "name": "Distribution scaffold notice",
                            "file": "licenses/DISTRIBUTION-NOTICE.txt"
                          }
                        ]
                      }
                    }
                """.trimIndent(),
            )
            installDir.resolve("model").mkdirs()
            installDir.resolve("licenses").mkdirs()
            installDir.resolve("model/model.onnx").writeText("fake")
            installDir.resolve("model/tokens.txt").writeText("fake")
            installDir.resolve("licenses/DISTRIBUTION-NOTICE.txt").writeText("fake")

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
