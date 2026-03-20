package com.yueliangmanle.danci.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.database.DanciDatabase
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.pronunciation.NativeVoicePackManifest
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
class VoicePackRepositoryTest {
    @Test
    fun syncManifestPreservesChecksumsUrlAcrossDatabaseRoundTrip() = runTest {
        withRepository { repository ->
            repository.syncManifest(
                """
                    {
                      "voicePacks": [
                        {
                          "id": "custom-offline-word-v1",
                          "name": "自定义离线发音包",
                          "locale": "en-US",
                          "accent": "us",
                          "engineType": "sherpa_onnx",
                          "version": "1.4.0",
                          "downloadUrl": "https://example.com/packs/custom-offline-word-v1.zip",
                          "manifestUrl": "https://example.com/packs/custom-offline-word-v1-manifest.json",
                          "checksumsUrl": "https://example.com/packs/custom-voice-pack-checksums.txt",
                          "native": {
                            "engineType": "native_neural_tts",
                            "modelFamily": "kokoro",
                            "modelVersion": "1.4.0",
                            "packageFormatVersion": 2,
                            "supportsImportedWords": true,
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
                        }
                      ]
                    }
                """.trimIndent(),
            )

            assertEquals(
                "https://example.com/packs/custom-voice-pack-checksums.txt",
                repository.getVoicePack("custom-offline-word-v1")?.checksumsUrl,
            )
        }
    }

    @Test
    fun refreshCatalogHydratesRuntimeChecksumsUrlForStoredVoicePack() = runTest {
        withRepository { repository ->
            repository.refreshCatalog()

            val pack = repository.getVoicePack("en-gb-offline-word-v1")

            assertEquals(
                "https://github.com/yuelangmanle/wenwenlex/releases/latest/download/wenwenlex-voice-pack-checksums.txt",
                pack?.checksumsUrl,
            )
        }
    }

    @Test
    fun nativeManifestFromCatalogPrefersNativeBlockEngineType() {
        val manifest = NativeVoicePackManifest.fromCatalogItem(
            JSONObject(
                """
                    {
                      "id": "en-gb-offline-word-v1",
                      "engineType": "sherpa_onnx",
                      "native": {
                        "engineType": "native_neural_tts",
                        "modelFamily": "kokoro"
                      }
                    }
                """.trimIndent(),
            ),
        )

        assertEquals("native_neural_tts", manifest.engineType)
    }

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
            currentActiveIdsByAccent = mapOf("us" to "en-us-bridge-basic"),
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
            currentActiveIdsByAccent = emptyMap(),
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
                      "checksumsUrl": "https://example.com/packs/voice-pack-checksums.txt",
                      "archiveChecksum": "abc123",
                      "native": {
                        "engineType": "native_neural_tts",
                        "modelFamily": "kokoro",
                        "modelVersion": "1.4.0",
                        "packageFormatVersion": 2,
                        "supportsImportedWords": true,
                        "entryFiles": [
                          "model/model.onnx",
                          "model/tokens.txt"
                        ],
                        "payloadChecksums": {
                          "model/model.onnx": "aaa",
                          "model/tokens.txt": "bbb"
                        },
                        "estimatedStorageBytes": 612345678,
                        "estimatedRamMb": 768,
                        "speakerProfile": "offline_word",
                        "licenses": [
                          {
                            "spdx": "Apache-2.0",
                            "file": "licenses/Apache-2.0.txt"
                          },
                          {
                            "name": "Distribution scaffold notice",
                            "file": "licenses/DISTRIBUTION-NOTICE.txt"
                          }
                        ]
                      }
                    }
                  ]
                }
            """.trimIndent(),
            existingById = existing,
            currentActiveIdsByAccent = mapOf("uk" to "en-gb-offline-word-v1"),
            now = Instant.parse("2026-03-19T12:00:00Z"),
        )

        assertEquals(1, parsed.size)
        val pack = parsed.single()
        assertEquals("native_neural_tts", pack.engineFamily)
        assertEquals("kokoro", pack.modelFamily)
        assertTrue(pack.supportsImportedWords)
        assertEquals(612345678L, pack.estimatedStorageBytes)
        assertEquals(768, pack.estimatedRamMb)
        assertEquals(
            listOf("Apache-2.0", "Distribution scaffold notice"),
            pack.licenses,
        )
        assertEquals("https://example.com/packs/voice-pack-checksums.txt", pack.checksumsUrl)
        assertEquals("/tmp/en-gb", pack.installDir)
        assertEquals(512_000_000L, pack.installedSizeBytes)
        assertEquals(VoicePackStatus.READY.storageValue, pack.status)
        assertTrue(pack.isActive)
    }

    @Test
    fun activateVoicePack_keepsActivePackOfOtherAccent() = runTest {
        withRepository { repository ->
            repository.upsertVoicePack(
                TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    locale = "en-GB",
                    accent = "uk",
                    status = VoicePackStatus.READY.storageValue,
                ),
            )
            repository.upsertVoicePack(
                TestVoicePackFactory.voicePack(
                    id = "en-us-offline-word-v1",
                    locale = "en-US",
                    accent = "us",
                    status = VoicePackStatus.READY.storageValue,
                ),
            )

            repository.activateVoicePack("en-gb-offline-word-v1")
            repository.activateVoicePack("en-us-offline-word-v1")

            val packs = repository.getAllVoicePacks()
            assertTrue(packs.single { it.id == "en-gb-offline-word-v1" }.isActive)
            assertTrue(packs.single { it.id == "en-us-offline-word-v1" }.isActive)
        }
    }

    @Test
    fun removeVoicePack_clearsOnlyRemovedAccentActiveState() = runTest {
        withRepository { repository ->
            repository.upsertVoicePack(
                TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    locale = "en-GB",
                    accent = "uk",
                    status = VoicePackStatus.READY.storageValue,
                    installDir = tempInstallDir("en-gb-offline-word-v1"),
                ),
            )
            repository.upsertVoicePack(
                TestVoicePackFactory.voicePack(
                    id = "en-us-offline-word-v1",
                    locale = "en-US",
                    accent = "us",
                    status = VoicePackStatus.READY.storageValue,
                    installDir = tempInstallDir("en-us-offline-word-v1"),
                ),
            )

            repository.activateVoicePack("en-gb-offline-word-v1")
            repository.activateVoicePack("en-us-offline-word-v1")
            repository.removeVoicePack("en-gb-offline-word-v1")

            val packs = repository.getAllVoicePacks()
            assertNull(packs.firstOrNull { it.id == "en-gb-offline-word-v1" })
            assertTrue(packs.single { it.id == "en-us-offline-word-v1" }.isActive)
        }
    }

    @Test
    fun getActiveVoicePack_withAutoAccentReturnsNullWhenMultipleAccentPacksAreActive() = runTest {
        withRepository { repository ->
            repository.upsertVoicePack(
                TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    locale = "en-GB",
                    accent = "uk",
                    status = VoicePackStatus.READY.storageValue,
                ),
            )
            repository.upsertVoicePack(
                TestVoicePackFactory.voicePack(
                    id = "en-us-offline-word-v1",
                    locale = "en-US",
                    accent = "us",
                    status = VoicePackStatus.READY.storageValue,
                ),
            )

            repository.activateVoicePack("en-gb-offline-word-v1")
            repository.activateVoicePack("en-us-offline-word-v1")

            assertNull(repository.getActiveVoicePack(PronunciationAccent.AUTO))
        }
    }

    @Test
    fun getActiveVoicePack_withAutoAccentReturnsPackWhenOnlyOneActivePackExists() = runTest {
        withRepository { repository ->
            repository.upsertVoicePack(
                TestVoicePackFactory.voicePack(
                    id = "en-gb-offline-word-v1",
                    locale = "en-GB",
                    accent = "uk",
                    status = VoicePackStatus.READY.storageValue,
                ),
            )

            repository.activateVoicePack("en-gb-offline-word-v1")

            assertEquals(
                "en-gb-offline-word-v1",
                repository.getActiveVoicePack(PronunciationAccent.AUTO)?.id,
            )
        }
    }

    private suspend fun withRepository(
        block: suspend (RoomVoicePackRepository) -> Unit,
    ) {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(appContext, DanciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            block(
                RoomVoicePackRepository(
                    appContext = appContext,
                    dao = database.voicePackDao(),
                    database = database,
                ),
            )
        } finally {
            database.close()
        }
    }

    private fun tempInstallDir(name: String): String {
        val root = createTempDir(prefix = "voice-pack-test-")
        val target = root.resolve(name)
        target.mkdirs()
        return target.absolutePath
    }
}
