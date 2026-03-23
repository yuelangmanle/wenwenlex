package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.AI_PROVIDER_TYPE_MIMO_TTS
import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.model.MIMO_TTS_BASE_URL
import com.yueliangmanle.danci.core.model.MIMO_TTS_MODEL
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiHealthCheckerTest {
    @Test
    fun check_returnsFailureWhenMinimalTtsCallRejected() = runTest {
        val checker = ApiHealthChecker(
            miMoTtsProvider = MiMoTtsProvider(
                transport = FakeCloudTtsTransport(
                    response = CloudTtsHttpResponse(
                        statusCode = 401,
                        body = """{"error":{"message":"invalid api key"}}""",
                    ),
                ),
            ),
        )

        val result = checker.check(
            profile = AiProviderProfile(
                id = "profile-mimo",
                name = "MiMo",
                providerType = AI_PROVIDER_TYPE_MIMO_TTS,
                baseUrl = MIMO_TTS_BASE_URL,
                model = MIMO_TTS_MODEL,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            apiKey = "bad-key",
            voice = "default_en",
        )

        assertFalse(result.success)
        assertTrue(result.summary.contains("401"))
    }
}

