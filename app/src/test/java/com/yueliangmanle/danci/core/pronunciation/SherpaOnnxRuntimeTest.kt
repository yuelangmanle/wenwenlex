package com.yueliangmanle.danci.core.pronunciation

import java.io.File
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
}
