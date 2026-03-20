package com.yueliangmanle.danci.core.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoicePackInstallerTest {
    @Test
    fun validateInstalledVoicePackRejectsNativeManifestWithoutLicenses() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            val modelChecksum = sha256ForTest("fake".toByteArray())
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-us-offline-word-v1",
                      "name": "美式离线发音包",
                      "accent": "us",
                      "locale": "en-US",
                      "engineType": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "modelVersion": "1.4.0",
                      "packageFormatVersion": 2,
                      "entryFiles": ["model.onnx"],
                      "payloadChecksums": {
                        "model.onnx": "$modelChecksum"
                      },
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "speakerProfile": "offline_word",
                      "licenses": []
                    }
                """.trimIndent(),
            )
            installDir.resolve("model.onnx").writeText("fake")

            assertValidationFailure(
                installDir = installDir,
                expectedMessage = "licenses",
            )
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
                      "name": "美式离线发音包",
                      "accent": "us",
                      "locale": "en-US",
                      "engineType": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "modelVersion": "1.4.0",
                      "packageFormatVersion": 2,
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "speakerProfile": "offline_word",
                      "licenses": [
                        {
                          "spdx": "Apache-2.0"
                        }
                      ]
                    }
                """.trimIndent(),
            )

            assertValidationFailure(
                installDir = installDir,
                expectedMessage = "entryFiles",
            )
        } finally {
            installDir.deleteRecursively()
        }
    }

    @Test
    fun validateInstalledVoicePackRejectsMissingReferencedLicenseFile() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            val modelChecksum = sha256ForTest("fake-model".toByteArray())
            val tokensChecksum = sha256ForTest("fake-tokens".toByteArray())
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-us-offline-word-v1",
                      "name": "美式离线发音包",
                      "accent": "us",
                      "locale": "en-US",
                      "engineType": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "modelVersion": "1.4.0",
                      "packageFormatVersion": 2,
                      "entryFiles": ["model/model.onnx", "model/tokens.txt"],
                      "payloadChecksums": {
                        "model/model.onnx": "$modelChecksum",
                        "model/tokens.txt": "$tokensChecksum"
                      },
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "speakerProfile": "offline_word",
                      "licenses": [
                        {
                          "name": "Distribution scaffold notice",
                          "file": "licenses/DISTRIBUTION-NOTICE.txt"
                        }
                      ]
                    }
                """.trimIndent(),
            )
            installDir.resolve("model").mkdirs()
            installDir.resolve("model/model.onnx").writeText("fake-model")
            installDir.resolve("model/tokens.txt").writeText("fake-tokens")

            assertValidationFailure(
                installDir = installDir,
                expectedMessage = "licenses/DISTRIBUTION-NOTICE.txt",
            )
        } finally {
            installDir.deleteRecursively()
        }
    }

    @Test
    fun validateInstalledVoicePackRejectsManifestIdMismatch() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            val modelChecksum = sha256ForTest("fake-model".toByteArray())
            val tokensChecksum = sha256ForTest("fake-tokens".toByteArray())
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-gb-offline-word-v1",
                      "name": "英式离线发音包",
                      "accent": "uk",
                      "locale": "en-GB",
                      "engineType": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "modelVersion": "1.4.0",
                      "packageFormatVersion": 2,
                      "entryFiles": ["model/model.onnx", "model/tokens.txt"],
                      "payloadChecksums": {
                        "model/model.onnx": "$modelChecksum",
                        "model/tokens.txt": "$tokensChecksum"
                      },
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "speakerProfile": "offline_word",
                      "licenses": [
                        {
                          "name": "Distribution scaffold notice",
                          "file": "licenses/DISTRIBUTION-NOTICE.txt"
                        }
                      ]
                    }
                """.trimIndent(),
            )
            installDir.resolve("model").mkdirs()
            installDir.resolve("licenses").mkdirs()
            installDir.resolve("model/model.onnx").writeText("fake-model")
            installDir.resolve("model/tokens.txt").writeText("fake-tokens")
            installDir.resolve("licenses/DISTRIBUTION-NOTICE.txt").writeText("fake-license")

            assertValidationFailure(
                installDir = installDir,
                expectedMessage = "不匹配",
                voicePack = testVoicePack(
                    installDir = installDir,
                    id = "en-us-offline-word-v1",
                    locale = "en-US",
                    accent = "us",
                ),
            )
        } finally {
            installDir.deleteRecursively()
        }
    }

    @Test
    fun validateInstalledVoicePackRejectsPayloadPathTraversal() {
        val parentDir = Files.createTempDirectory("voice-pack-native-parent").toFile()
        val installDir = File(parentDir, "install").apply { mkdirs() }
        val outsideDir = File(parentDir, "escaped").apply { mkdirs() }
        val outsideFile = File(outsideDir, "model.onnx").apply { writeText("escaped-model") }
        val outsideChecksum = sha256ForTest(outsideFile.readBytes())
        try {
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-us-offline-word-v1",
                      "name": "美式离线发音包",
                      "accent": "us",
                      "locale": "en-US",
                      "engineType": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "modelVersion": "1.4.0",
                      "packageFormatVersion": 2,
                      "entryFiles": ["../escaped/model.onnx"],
                      "payloadChecksums": {
                        "../escaped/model.onnx": "$outsideChecksum"
                      },
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "speakerProfile": "offline_word",
                      "licenses": [
                        {
                          "name": "Distribution scaffold notice",
                          "file": "licenses/DISTRIBUTION-NOTICE.txt"
                        }
                      ]
                    }
                """.trimIndent(),
            )
            installDir.resolve("licenses").mkdirs()
            installDir.resolve("licenses/DISTRIBUTION-NOTICE.txt").writeText("fake-license")

            assertValidationFailure(
                installDir = installDir,
                expectedMessage = "非法",
            )
        } finally {
            parentDir.deleteRecursively()
        }
    }

    @Test
    fun validateInstalledVoicePackAcceptsPackagedNativeReleaseStructure() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            val modelChecksum = sha256ForTest("fake-model".toByteArray())
            val tokensChecksum = sha256ForTest("fake-tokens".toByteArray())
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-gb-offline-word-v1",
                      "name": "英式离线发音包",
                      "accent": "uk",
                      "locale": "en-GB",
                      "engineType": "native_neural_tts",
                      "modelFamily": "kokoro",
                      "modelVersion": "1.4.0",
                      "packageFormatVersion": 2,
                      "entryFiles": ["model/model.onnx", "model/tokens.txt"],
                      "payloadChecksums": {
                        "model/model.onnx": "$modelChecksum",
                        "model/tokens.txt": "$tokensChecksum"
                      },
                      "estimatedStorageBytes": 123,
                      "estimatedRamMb": 456,
                      "speakerProfile": "offline_word",
                      "licenses": [
                        {
                          "name": "Distribution scaffold notice",
                          "file": "licenses/DISTRIBUTION-NOTICE.txt"
                        }
                      ]
                    }
                """.trimIndent(),
            )
            installDir.resolve("model").mkdirs()
            installDir.resolve("licenses").mkdirs()
            installDir.resolve("model/model.onnx").writeText("fake-model")
            installDir.resolve("model/tokens.txt").writeText("fake-tokens")
            installDir.resolve("licenses/DISTRIBUTION-NOTICE.txt").writeText("fake-license")

            validateInstalledVoicePack(
                voicePack = testVoicePack(
                    installDir = installDir,
                    id = "en-gb-offline-word-v1",
                    locale = "en-GB",
                    accent = "uk",
                ),
                installDir = installDir,
            )
        } finally {
            installDir.deleteRecursively()
        }
    }

    @Test
    fun validateInstalledVoicePackAcceptsLegacyNestedNativeManifestWithPayloadChecksums() {
        val installDir = Files.createTempDirectory("voice-pack-native-test").toFile()
        try {
            val modelChecksum = sha256ForTest("fake-model".toByteArray())
            val tokensChecksum = sha256ForTest("fake-tokens".toByteArray())
            installDir.resolve("manifest.json").writeText(
                """
                    {
                      "id": "en-gb-offline-word-v1",
                      "entryFiles": ["model/model.onnx", "model/tokens.txt"],
                      "payloadChecksums": {
                        "model/model.onnx": "$modelChecksum",
                        "model/tokens.txt": "$tokensChecksum"
                      },
                      "native": {
                        "engineFamily": "native_neural_tts",
                        "modelFamily": "kokoro",
                        "modelVersion": "1.4.0",
                        "packageFormatVersion": 2,
                        "estimatedStorageBytes": 123,
                        "estimatedRamMb": 456,
                        "speakerProfile": "offline_word",
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
            installDir.resolve("model/model.onnx").writeText("fake-model")
            installDir.resolve("model/tokens.txt").writeText("fake-tokens")
            installDir.resolve("licenses/DISTRIBUTION-NOTICE.txt").writeText("fake-license")

            validateInstalledVoicePack(
                voicePack = testVoicePack(
                    installDir = installDir,
                    id = "en-gb-offline-word-v1",
                    locale = "en-GB",
                    accent = "uk",
                ),
                installDir = installDir,
            )
        } finally {
            installDir.deleteRecursively()
        }
    }

    @Test
    fun install_failsWhenPayloadChecksumDoesNotMatchManifest() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val cacheDir = Files.createTempDirectory("voice-pack-cache").toFile()
        val installRootDir = Files.createTempDirectory("voice-pack-install-root").toFile()
        val archiveName = "wenwenlex-voice-pack-en-us-offline-word-v1.zip"
        val archiveFile = File(cacheDir, archiveName)
        val expectedPayloadChecksum = sha256ForTest("expected-model".toByteArray())
        archiveFile.writeZip(
            "manifest.json" to """
                {
                  "id": "en-us-offline-word-v1",
                  "name": "美式离线发音包",
                  "accent": "us",
                  "locale": "en-US",
                  "engineType": "native_neural_tts",
                  "modelFamily": "kokoro",
                  "modelVersion": "1.4.0",
                  "packageFormatVersion": 2,
                  "entryFiles": ["model/model.onnx"],
                  "payloadChecksums": {
                    "model/model.onnx": "$expectedPayloadChecksum"
                  },
                  "estimatedStorageBytes": 123,
                  "estimatedRamMb": 456,
                  "speakerProfile": "offline_word",
                  "licenses": [
                    {
                      "name": "Distribution scaffold notice",
                      "file": "licenses/DISTRIBUTION-NOTICE.txt"
                    }
                  ]
                }
            """.trimIndent().toByteArray(),
            "model/model.onnx" to "actual-model".toByteArray(),
            "licenses/DISTRIBUTION-NOTICE.txt" to "license".toByteArray(),
        )
        val archiveChecksum = sha256ForTest(archiveFile.readBytes())
        val baseUrl = "https://voice-pack.test"
        val remoteFetcher = FakeVoicePackRemoteFetcher(
            routes = mapOf(
                "$baseUrl/$archiveName" to archiveFile.readBytes(),
                "$baseUrl/voice-pack-checksums.txt" to "$archiveChecksum  $archiveName\n".toByteArray(),
            ),
        )

        try {
            val installer = VoicePackInstaller(
                assetManager = appContext.assets,
                cacheDir = cacheDir,
                installRootDir = installRootDir,
                remoteFetcher = remoteFetcher,
            )
            val pack = VoicePack(
                id = "en-us-offline-word-v1",
                name = "美式离线发音包",
                locale = "en-US",
                accent = "us",
                engineType = "sherpa_onnx",
                version = "1.4.0",
                downloadUrl = "$baseUrl/$archiveName",
                checksumsUrl = "$baseUrl/voice-pack-checksums.txt",
            )

            var error: IllegalStateException? = null
            try {
                installer.install(voicePack = pack, onStatusChange = {})
                fail("Expected payload checksum mismatch to fail installation.")
            } catch (expected: IllegalStateException) {
                error = expected
            }
            assertTrue(error?.message.orEmpty().contains("payload checksum"))
        } finally {
            cacheDir.deleteRecursively()
            installRootDir.deleteRecursively()
        }
    }

    @Test
    fun install_usesDownloadUrlArchiveNameWhenMatchingReleaseChecksumsIndex() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val cacheDir = Files.createTempDirectory("voice-pack-cache").toFile()
        val installRootDir = Files.createTempDirectory("voice-pack-install-root").toFile()
        val archiveName = "wenwenlex-voice-pack-en-us-offline-word-v1.zip"
        val archiveFile = File(cacheDir, archiveName)
        archiveFile.writeZip(
            "manifest.json" to validNativeManifest("en-us-offline-word-v1", "us", "en-US").toByteArray(),
            "model/model.onnx" to "fake-model".toByteArray(),
            "model/tokens.txt" to "fake-tokens".toByteArray(),
            "licenses/DISTRIBUTION-NOTICE.txt" to "fake-license".toByteArray(),
        )
        val archiveChecksum = sha256ForTest(archiveFile.readBytes())
        val baseUrl = "https://voice-pack.test/downloads"
        val remoteFetcher = FakeVoicePackRemoteFetcher(
            routes = mapOf(
                "$baseUrl/$archiveName" to archiveFile.readBytes(),
                "$baseUrl/voice-pack-checksums.txt" to """
                    $archiveChecksum  $archiveName
                    deadbeef  en-us-offline-word-v1.zip
                """.trimIndent().toByteArray(),
            ),
        )

        try {
            val installer = VoicePackInstaller(
                assetManager = appContext.assets,
                cacheDir = cacheDir,
                installRootDir = installRootDir,
                remoteFetcher = remoteFetcher,
            )
            val statuses = mutableListOf<String>()

            installer.install(
                voicePack = TestVoicePackFactory.voicePack(
                    id = "en-us-offline-word-v1",
                    name = "美式离线发音包",
                    locale = "en-US",
                    accent = "us",
                    engineType = "sherpa_onnx",
                    version = "1.4.0",
                    downloadUrl = "$baseUrl/$archiveName",
                    checksumsUrl = "$baseUrl/voice-pack-checksums.txt",
                    archiveChecksum = "ffffffff",
                ),
                onStatusChange = statuses::add,
            )

            assertEquals(
                listOf(
                    VoicePackStatus.DOWNLOADING.storageValue,
                    VoicePackStatus.VERIFYING.storageValue,
                    VoicePackStatus.INSTALLING.storageValue,
                ),
                statuses.distinct(),
            )
        } finally {
            cacheDir.deleteRecursively()
            installRootDir.deleteRecursively()
        }
    }

    @Test
    fun install_fallsBackToLegacyArchiveChecksumWhenChecksumsUrlMissing() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val cacheDir = Files.createTempDirectory("voice-pack-cache").toFile()
        val installRootDir = Files.createTempDirectory("voice-pack-install-root").toFile()
        val archiveName = "wenwenlex-voice-pack-en-gb-offline-word-v1.zip"
        val archiveFile = File(cacheDir, archiveName)
        archiveFile.writeZip(
            "manifest.json" to validNativeManifest("en-gb-offline-word-v1", "uk", "en-GB").toByteArray(),
            "model/model.onnx" to "fake-model".toByteArray(),
            "model/tokens.txt" to "fake-tokens".toByteArray(),
            "licenses/DISTRIBUTION-NOTICE.txt" to "fake-license".toByteArray(),
        )
        val archiveChecksum = sha256ForTest(archiveFile.readBytes())
        val baseUrl = "https://voice-pack.test"
        val remoteFetcher = FakeVoicePackRemoteFetcher(
            routes = mapOf(
                "$baseUrl/$archiveName" to archiveFile.readBytes(),
            ),
        )

        try {
            val installer = VoicePackInstaller(
                assetManager = appContext.assets,
                cacheDir = cacheDir,
                installRootDir = installRootDir,
                remoteFetcher = remoteFetcher,
            )

            installer.install(
                voicePack = TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    name = "英式离线发音包",
                    locale = "en-GB",
                    accent = "uk",
                    engineType = "sherpa_onnx",
                    version = "1.4.0",
                    downloadUrl = "$baseUrl/$archiveName",
                    checksumsUrl = null,
                    archiveChecksum = archiveChecksum,
                ),
                onStatusChange = {},
            )
        } finally {
            cacheDir.deleteRecursively()
            installRootDir.deleteRecursively()
        }
    }

    private fun assertValidationFailure(
        installDir: File,
        expectedMessage: String,
        voicePack: VoicePack = testVoicePack(installDir),
    ) {
        var error: IllegalStateException? = null
        try {
            validateInstalledVoicePack(
                voicePack = voicePack,
                installDir = installDir,
            )
            fail("Expected native pack validation to fail.")
        } catch (expected: IllegalStateException) {
            error = expected
        }

        assertTrue(error?.message.orEmpty().contains(expectedMessage))
    }

    private fun testVoicePack(
        installDir: File,
        id: String = "en-us-offline-word-v1",
        locale: String = "en-US",
        accent: String = "us",
    ) =
        TestVoicePackFactory.voicePack(
            id = id,
            locale = locale,
            accent = accent,
            engineType = "sherpa_onnx",
            installDir = installDir.absolutePath,
            status = VoicePackStatus.READY.storageValue,
        )
}

private fun File.writeZip(vararg entries: Pair<String, ByteArray>) {
    outputStream().buffered().use { fileOutput ->
        ZipOutputStream(fileOutput).use { zipOutput ->
            entries.forEach { (path, bytes) ->
                zipOutput.putNextEntry(ZipEntry(path))
                zipOutput.write(bytes)
                zipOutput.closeEntry()
            }
        }
    }
}

private fun sha256ForTest(bytes: ByteArray): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

private fun validNativeManifest(
    id: String,
    accent: String,
    locale: String,
): String {
    val modelChecksum = sha256ForTest("fake-model".toByteArray())
    val tokensChecksum = sha256ForTest("fake-tokens".toByteArray())
    return """
        {
          "id": "$id",
          "name": "$id",
          "accent": "$accent",
          "locale": "$locale",
          "engineType": "native_neural_tts",
          "modelFamily": "kokoro",
          "modelVersion": "1.4.0",
          "packageFormatVersion": 2,
          "entryFiles": ["model/model.onnx", "model/tokens.txt"],
          "payloadChecksums": {
            "model/model.onnx": "$modelChecksum",
            "model/tokens.txt": "$tokensChecksum"
          },
          "estimatedStorageBytes": 123,
          "estimatedRamMb": 456,
          "speakerProfile": "offline_word",
          "licenses": [
            {
              "name": "Distribution scaffold notice",
              "file": "licenses/DISTRIBUTION-NOTICE.txt"
            }
          ]
        }
    """.trimIndent()
}

private class FakeVoicePackRemoteFetcher(
    private val routes: Map<String, ByteArray>,
) : VoicePackRemoteFetcher {
    override fun downloadBytes(remoteUrl: String): ByteArray =
        routes[remoteUrl] ?: error("Missing fake remote payload for $remoteUrl")

    override fun downloadText(remoteUrl: String): String =
        downloadBytes(remoteUrl).toString(Charsets.UTF_8)
}
