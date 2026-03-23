package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.VoicePackRepository
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationSessionPreference
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.VoicePack
import com.yueliangmanle.danci.core.model.VoicePackStatus
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationSourceRegistryTest {
    @Test
    fun setDefaultWordSource_updatesEntityFlagsAndKeepsLongTextDefaultUntouched() = runTest {
        val repository = InMemoryPronunciationSourceRepository(
            mutableListOf(
                testSource(
                    id = "dictionary-uk",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.UK,
                    isDefaultForWord = true,
                ),
                testSource(
                    id = "dictionary-us",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.US,
                ),
                testSource(
                    id = "native-us",
                    type = PronunciationSourceType.LOCAL_NATIVE,
                    accent = PronunciationAccent.US,
                    isDefaultForLongText = true,
                ),
            ),
        )

        repository.setDefaultWordSource("dictionary-us")
        val sources = repository.getAllSources()

        assertTrue(sources.single { it.id == "dictionary-us" }.isDefaultForWord)
        assertTrue(sources.any { it.isDefaultForLongText })
        assertTrue(sources.single { it.id == "native-us" }.isDefaultForLongText)
    }

    @Test
    fun resolveCurrentWordSource_usesSessionSourceAndAdaptsAccent() = runTest {
        val repository = InMemoryPronunciationSourceRepository(
            mutableListOf(
                testSource(
                    id = "dictionary-uk",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.UK,
                    isDefaultForWord = true,
                ),
                testSource(
                    id = "dictionary-us",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.US,
                ),
                testSource(
                    id = "native-uk",
                    type = PronunciationSourceType.LOCAL_NATIVE,
                    accent = PronunciationAccent.UK,
                    backingVoicePackId = "native-uk",
                ),
                testSource(
                    id = "native-us",
                    type = PronunciationSourceType.LOCAL_NATIVE,
                    accent = PronunciationAccent.US,
                    backingVoicePackId = "native-us",
                ),
            ),
        )
        val settingsRepository = FakeRegistrySettingsRepository(
            sessionPreference = PronunciationSessionPreference(
                sessionWordPronunciationSourceId = "native-uk",
            ),
        )
        val registry = PronunciationSourceRegistry(
            sourceRepository = repository,
            voicePackRepository = FakeRegistryVoicePackRepository(),
            settingsRepository = settingsRepository,
        )

        val resolved = registry.resolveCurrentWordSource(PronunciationAccent.US)

        assertEquals("native-us", resolved.id)
        assertEquals(PronunciationAccent.US.storageValue, resolved.accent)
    }
}

private fun testSource(
    id: String,
    type: PronunciationSourceType,
    accent: PronunciationAccent,
    isDefaultForWord: Boolean = false,
    isDefaultForLongText: Boolean = false,
    backingVoicePackId: String? = null,
): PronunciationSource =
    PronunciationSource(
        id = id,
        name = id,
        sourceType = type.storageValue,
        accent = accent.storageValue,
        enabled = true,
        isDefaultForWord = isDefaultForWord,
        isDefaultForLongText = isDefaultForLongText,
        backingVoicePackId = backingVoicePackId,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

private class InMemoryPronunciationSourceRepository(
    private val sources: MutableList<PronunciationSource> = mutableListOf(),
) : PronunciationSourceRepository {
    override suspend fun getAllSources(): List<PronunciationSource> = sources.toList()

    override suspend fun getSource(sourceId: String): PronunciationSource? =
        sources.firstOrNull { it.id == sourceId }

    override suspend fun upsertSources(sources: List<PronunciationSource>) {
        sources.forEach { source ->
            val index = this.sources.indexOfFirst { it.id == source.id }
            if (index >= 0) {
                this.sources[index] = source
            } else {
                this.sources += source
            }
        }
    }

    override suspend fun setDefaultWordSource(sourceId: String) {
        val index = sources.indexOfFirst { it.id == sourceId }
        if (index < 0) return
        sources.indices.forEach { itemIndex ->
            val source = sources[itemIndex]
            sources[itemIndex] = source.copy(
                isDefaultForWord = source.id == sourceId,
            )
        }
    }

    override suspend fun setDefaultLongTextSource(sourceId: String) {
        val index = sources.indexOfFirst { it.id == sourceId }
        if (index < 0) return
        sources.indices.forEach { itemIndex ->
            val source = sources[itemIndex]
            sources[itemIndex] = source.copy(
                isDefaultForLongText = source.id == sourceId,
            )
        }
    }

    override suspend fun clearAll() {
        sources.clear()
    }
}

private class FakeRegistryVoicePackRepository : VoicePackRepository {
    override suspend fun getAllVoicePacks(): List<VoicePack> = emptyList()
    override suspend fun getVoicePack(id: String): VoicePack? = null
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

private class FakeRegistrySettingsRepository(
    initialSettings: AppSettings = AppSettings(),
    private var sessionPreference: PronunciationSessionPreference = PronunciationSessionPreference(),
) : SettingsRepository {
    private val state = MutableStateFlow(initialSettings)

    override val settings: Flow<AppSettings> = state

    override suspend fun getSettings(): AppSettings = state.value

    override suspend fun getPronunciationSessionPreference(): PronunciationSessionPreference = sessionPreference

    override suspend fun updateSessionWordPronunciationSourceId(sourceId: String?) {
        sessionPreference = sessionPreference.copy(sessionWordPronunciationSourceId = sourceId)
    }

    override suspend fun updateSessionLongTextPronunciationSourceId(sourceId: String?) {
        sessionPreference = sessionPreference.copy(sessionLongTextPronunciationSourceId = sourceId)
    }

    override suspend fun updateDailyGoal(dailyGoal: Int) = Unit
    override suspend fun updateWeeklyGoal(weeklyGoal: Int) = Unit
    override suspend fun updatePhaseName(phaseName: String?) = Unit
    override suspend fun updatePhaseTargetWords(phaseTargetWords: Int) = Unit
    override suspend fun updateActiveBookId(bookId: String?) = Unit
    override suspend fun updateAiEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiBaseUrl(baseUrl: String) = Unit
    override suspend fun updateAiModel(model: String) = Unit
    override suspend fun updateDefaultAiProfileId(profileId: String?) = Unit
    override suspend fun updateWordHelpProfileId(profileId: String?) = Unit
    override suspend fun updatePlanAdjustmentProfileId(profileId: String?) = Unit
    override suspend fun updatePhoneticFillProfileId(profileId: String?) = Unit
    override suspend fun updateAiPlanAdjustmentEnabled(enabled: Boolean) = Unit
    override suspend fun updateAiSessionCheckpointEnabled(enabled: Boolean) = Unit
    override suspend fun updatePreferredPronunciationAccent(accent: String) = Unit
    override suspend fun updatePronunciationMode(mode: String) = Unit
    override suspend fun updateAllowCellularVoicePackDownload(enabled: Boolean) = Unit
    override suspend fun updateAutoCacheWordAudio(enabled: Boolean) = Unit
    override suspend fun updateAudioCacheLimitMb(limitMb: Int) = Unit
    override suspend fun updateActiveVoicePackId(voicePackId: String?) = Unit
    override suspend fun updateFallbackToSystemTts(enabled: Boolean) = Unit
    override suspend fun updatePreferOfflineForLongText(enabled: Boolean) = Unit
    override suspend fun updateReminderEnabled(enabled: Boolean) = Unit
    override suspend fun updateReminderTime(hour: Int, minute: Int) = Unit
}
