package com.yueliangmanle.danci.core.data

import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePackRepositoryTest {
    @Test
    fun parseVoicePackManifestPreservesExistingInstallState() {
        val existing = mapOf(
            "en-us-bridge-basic" to TestVoicePackFactory.voicePack(
                id = "en-us-bridge-basic",
                installDir = "/tmp/en-us",
                installedSizeBytes = 2048,
                status = VoicePackStatus.READY.storageValue,
            ),
        )

        val parsed = parseVoicePackManifest(
            jsonText = """
                {
                  "voicePacks": [
                    {
                      "id": "en-us-bridge-basic",
                      "name": "美式基础桥接包",
                      "locale": "en-US",
                      "accent": "us",
                      "engineType": "system_tts_bridge",
                      "version": "1",
                      "downloadUrl": "asset://pronunciation/packs/en-us-bridge-basic"
                    }
                  ]
                }
            """.trimIndent(),
            existingById = existing,
            currentActiveId = "en-us-bridge-basic",
            now = Instant.parse("2026-03-19T12:00:00Z"),
        )

        assertEquals(1, parsed.size)
        val pack = parsed.single()
        assertEquals("/tmp/en-us", pack.installDir)
        assertEquals(2048L, pack.installedSizeBytes)
        assertEquals(VoicePackStatus.READY.storageValue, pack.status)
        assertTrue(pack.isActive)
    }

    @Test
    fun parseVoicePackManifestAppliesDefaultsForSparsePayload() {
        val parsed = parseVoicePackManifest(
            jsonText = """
                {
                  "voicePacks": [
                    {
                      "id": "en-gb-basic"
                    }
                  ]
                }
            """.trimIndent(),
            existingById = emptyMap(),
            currentActiveId = null,
            now = Instant.parse("2026-03-19T12:00:00Z"),
        )

        val pack = parsed.single()
        assertEquals("en-gb-basic", pack.name)
        assertEquals("en-US", pack.locale)
        assertEquals("auto", pack.accent)
        assertEquals("sherpa_onnx", pack.engineType)
        assertEquals(VoicePackStatus.NOT_INSTALLED.storageValue, pack.status)
    }

    @Test
    fun parseVoicePackManifestReadsNativeMetadataWithoutDroppingInstallState() {
        val existing = mapOf(
            "en-gb-offline-word-v1" to TestVoicePackFactory.voicePack(
                id = "en-gb-offline-word-v1",
                installDir = "/tmp/en-gb",
                installedSizeBytes = 512_000_000L,
                status = VoicePackStatus.READY.storageValue,
            ),
        )

        val parsed = parseVoicePackManifest(
            jsonText = """
                {
                  "voicePacks": [
                    {
                      "id": "en-gb-offline-word-v1",
                      "name": "英式离线发音包",
                      "locale": "en-GB",
                      "accent": "uk",
                      "engineType": "sherpa_onnx",
                      "version": "1.3.0",
                      "downloadUrl": "https://example.com/packs/en-gb-offline-word-v1.zip",
                      "manifestUrl": "https://example.com/packs/en-gb-offline-word-v1-manifest.json",
                      "archiveChecksum": "abc123",
                      "native": {
                        "engineFamily": "native_neural_tts",
                        "modelFamily": "kokoro",
                        "supportsImportedWords": true,
                        "estimatedStorageBytes": 612345678,
                        "estimatedRamMb": 768,
                        "licenses": [
                          "Apache-2.0",
                          "MIT"
                        ]
                      }
                    }
                  ]
                }
            """.trimIndent(),
            existingById = existing,
            currentActiveId = "en-gb-offline-word-v1",
            now = Instant.parse("2026-03-19T12:00:00Z"),
        )

        assertEquals(1, parsed.size)
        val pack = parsed.single()
        assertEquals("native_neural_tts", pack.engineFamily)
        assertEquals("kokoro", pack.modelFamily)
        assertTrue(pack.supportsImportedWords)
        assertEquals(612345678L, pack.estimatedStorageBytes)
        assertEquals(768, pack.estimatedRamMb)
        assertEquals(listOf("Apache-2.0", "MIT"), pack.licenses)
        assertEquals("/tmp/en-gb", pack.installDir)
        assertEquals(512_000_000L, pack.installedSizeBytes)
        assertEquals(VoicePackStatus.READY.storageValue, pack.status)
        assertTrue(pack.isActive)
    }
}
