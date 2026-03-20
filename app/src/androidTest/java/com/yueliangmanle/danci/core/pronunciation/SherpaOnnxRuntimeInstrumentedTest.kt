package com.yueliangmanle.danci.core.pronunciation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SherpaOnnxRuntimeInstrumentedTest {
    @Test
    fun synthesizeWordWritesNonEmptyWavFileOnAndroid() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val runtimeDir = installFixtureIfPresent(appContext) ?: run {
            assumeTrue("缺少 sherpa runtime 仪器测试夹具，先跳过。", false)
            return
        }
        val runtime = SherpaOnnxRuntime.Loader(runtimeDir).load()
        val outputFile = File(appContext.cacheDir, "android-tests/sherpa-hello.wav")

        runtime.synthesizeWord("hello", outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 44L)
    }

    private fun installFixtureIfPresent(appContext: Context): File? {
        val fixtureAssets = appContext.assets.list(FIXTURE_ASSET_DIR).orEmpty()
        if (fixtureAssets.isEmpty()) {
            return null
        }

        val outputDir = File(appContext.filesDir, "test-fixtures/sherpa-runtime").apply {
            deleteRecursively()
            mkdirs()
        }
        copyAssetTree(appContext, FIXTURE_ASSET_DIR, outputDir)
        return outputDir
    }

    private fun copyAssetTree(
        context: Context,
        assetPath: String,
        targetDir: File,
    ) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            context.assets.open(assetPath).use { input ->
                targetDir.parentFile?.mkdirs()
                targetDir.outputStream().use(input::copyTo)
            }
            return
        }

        targetDir.mkdirs()
        children.forEach { child ->
            val childAssetPath = "$assetPath/$child"
            val childTarget = File(targetDir, child)
            copyAssetTree(context, childAssetPath, childTarget)
        }
    }

    private companion object {
        const val FIXTURE_ASSET_DIR = "pronunciation/sherpa-runtime-fixture"
    }
}
