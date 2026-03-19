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
}
