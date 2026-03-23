package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcePlaybackResolverTest {
    private val resolver = SourcePlaybackResolver()

    @Test
    fun resolvePreparedCacheLookup_mapsLocalNativeToOfflineGeneratedBucket() {
        val lookup = resolver.resolvePreparedCacheLookup(
            source = source(
                id = "native-us",
                sourceType = "local_native",
            ),
        )

        assertNotNull(lookup)
        assertEquals(PlaybackSource.OFFLINE_NATIVE_GENERATED, lookup?.playbackSource)
        assertEquals("local_native", lookup?.actualSourceType)
    }

    @Test
    fun resolvePreparedCacheLookup_mapsCloudTtsToOnlinePrebuiltBucket() {
        val lookup = resolver.resolvePreparedCacheLookup(
            source = source(
                id = "cloud-mimo",
                sourceType = "cloud_tts",
            ),
        )

        assertNotNull(lookup)
        assertEquals(PlaybackSource.ONLINE_PREBUILT_CACHE, lookup?.playbackSource)
        assertEquals("cloud_tts", lookup?.actualSourceType)
    }

    @Test
    fun resolveGenerationSupport_marksBridgeSourceAsUnsupported() {
        val support = resolver.resolveGenerationSupport(
            source = source(
                id = "bridge-us",
                sourceType = "local_bridge",
            ),
        )

        assertFalse(support.supported)
        assertTrue(requireNotNull(support.failureReason).contains("暂不支持"))
    }
}

private fun source(
    id: String,
    sourceType: String,
): PronunciationSource =
    PronunciationSource(
        id = id,
        name = id,
        sourceType = sourceType,
        accent = "us",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
