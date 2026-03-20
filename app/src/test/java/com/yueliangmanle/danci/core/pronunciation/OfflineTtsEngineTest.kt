package com.yueliangmanle.danci.core.pronunciation

import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.data.TestVoicePackFactory
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackStatus
import com.yueliangmanle.danci.core.model.Word
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineTtsEngineTest {
    @Test
    fun speakWord_doesNotUseGlobalActivePackForAccentSpecificRequest() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ukInstallDir = File(appContext.cacheDir, "offline-tts-uk-pack").apply { mkdirs() }
        val repository = TrackingOfflineVoicePackRepository(
            globalActivePack = TestVoicePackFactory.voicePack(
                id = "en-gb-bridge-basic",
                locale = "en-GB",
                accent = PronunciationAccent.UK.storageValue,
                engineType = "system_tts_bridge",
                status = VoicePackStatus.READY.storageValue,
                installDir = ukInstallDir.absolutePath,
                isActive = true,
            ),
        )
        val engine = OfflineTtsEngine(
            voicePackRepository = repository,
            bridgeSpeaker = SystemTtsEngine(appContext),
        )

        val result = engine.speakWord(
            word = Word(id = 1L, lemma = "color"),
            accent = PronunciationAccent.US,
        )

        assertNull(result)
        assertEquals(0, repository.globalRequests)
        assertEquals(listOf(PronunciationAccent.US), repository.accentRequests)
    }
}

private class TrackingOfflineVoicePackRepository(
    private val globalActivePack: VoicePack? = null,
    private val activeByAccent: Map<PronunciationAccent, VoicePack?> = emptyMap(),
) : VoicePackRepository {
    var globalRequests: Int = 0
        private set
    val accentRequests = mutableListOf<PronunciationAccent>()

    override suspend fun getAllVoicePacks(): List<VoicePack> =
        buildList {
            globalActivePack?.let(::add)
            activeByAccent.values.filterNotNull().forEach(::add)
        }.distinctBy(VoicePack::id)

    override suspend fun getVoicePack(id: String): VoicePack? =
        getAllVoicePacks().firstOrNull { it.id == id }

    override suspend fun getActiveVoicePack(): VoicePack? {
        globalRequests += 1
        return globalActivePack
    }

    override suspend fun getActiveVoicePack(accent: PronunciationAccent): VoicePack? {
        accentRequests += accent
        return when (accent) {
            PronunciationAccent.AUTO -> activeByAccent.values.filterNotNull().singleOrNull()
            else -> activeByAccent[accent]
        }
    }

    override suspend fun activateVoicePack(id: String) = Unit

    override suspend fun upsertVoicePack(voicePack: VoicePack) = Unit

    override suspend fun removeVoicePack(id: String) = Unit

    override suspend fun syncManifest(jsonText: String): Int = 0

    override suspend fun refreshCatalog(): Int = 0

    override suspend fun updateVoicePackStatus(
        id: String,
        status: String,
        installDir: String?,
        installedSizeBytes: Long?,
    ) = Unit

    override suspend fun markInstalled(
        id: String,
        installDir: String,
        installedSizeBytes: Long,
    ) = Unit

    override fun voicePackRootDir(): File = File("/tmp")
}
