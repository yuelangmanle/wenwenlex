package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.AI_PROVIDER_TYPE_MIMO_TTS
import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.model.MIMO_TTS_BASE_URL
import com.yueliangmanle.danci.core.model.MIMO_TTS_MODEL
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiMoTtsProviderTest {
    @Test
    fun buildRequestBody_usesAssistantMessageVoiceAndStyle() {
        val provider = MiMoTtsProvider(transport = FakeCloudTtsTransport())

        val json = JSONObject(
            provider.buildRequestBody(
                CloudTtsRequest(
                    profile = mimoProfile(),
                    apiKey = "test-key",
                    text = "abandon",
                    voice = "default_en",
                    style = "Happy",
                ),
            ),
        )

        assertEquals(MIMO_TTS_MODEL, json.getString("model"))
        assertEquals("wav", json.getJSONObject("audio").getString("format"))
        assertEquals("default_en", json.getJSONObject("audio").getString("voice"))
        val assistantMessage = json.getJSONArray("messages").getJSONObject(0)
        assertEquals("assistant", assistantMessage.getString("role"))
        assertTrue(assistantMessage.getString("content").startsWith("<style>Happy</style>"))
    }

    @Test
    fun synthesize_decodesBase64AudioData() = runTest {
        val transport = FakeCloudTtsTransport(
            response = CloudTtsHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "choices": [
                        {
                          "message": {
                            "audio": {
                              "data": "${Base64.getEncoder().encodeToString("wave-data".toByteArray())}"
                            }
                          }
                        }
                      ]
                    }
                """.trimIndent(),
            ),
        )
        val provider = MiMoTtsProvider(transport = transport)

        val result = provider.synthesize(
            CloudTtsRequest(
                profile = mimoProfile(),
                apiKey = "test-key",
                text = "ability",
                voice = "mimo_default",
                style = null,
            ),
        )

        assertEquals("audio/wav", result.mimeType)
        assertEquals("wave-data", result.audioBytes.toString(Charsets.UTF_8))
        assertTrue(transport.lastRequestUrl!!.endsWith("/chat/completions"))
        assertEquals("Bearer test-key", transport.lastHeaders!!["Authorization"])
    }
}

private fun mimoProfile(): AiProviderProfile =
    AiProviderProfile(
        id = "profile-mimo",
        name = "MiMo TTS",
        providerType = AI_PROVIDER_TYPE_MIMO_TTS,
        baseUrl = MIMO_TTS_BASE_URL,
        model = MIMO_TTS_MODEL,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
