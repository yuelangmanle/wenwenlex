package com.yueliangmanle.danci.feature.worddetail

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.NoOpStudyEventRecorder
import com.yueliangmanle.danci.core.data.PhoneticEnrichmentRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.model.PhoneticEnrichmentJob
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.PronunciationSessionPreference
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourceType
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.pronunciation.PronunciationSourceRegistry
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordDetailViewModelTest {
    @Test
    fun switchSessionPronunciationSource_updatesUiState() = runTest {
        val settingsRepository = DetailSettingsRepository(AppSettings())
        val sourceRepository = DetailSourceRepository(
            mutableListOf(
                detailSource(
                    id = "dictionary-uk",
                    type = PronunciationSourceType.DICTIONARY,
                    accent = PronunciationAccent.UK,
                    isDefaultForWord = true,
                ),
                detailSource(
                    id = "native-us",
                    type = PronunciationSourceType.LOCAL_NATIVE,
                    accent = PronunciationAccent.US,
                    backingVoicePackId = "native-us",
                ),
            ),
        )
        val registry = PronunciationSourceRegistry(
            sourceRepository = sourceRepository,
            voicePackRepository = DetailVoicePackRepository(),
            settingsRepository = settingsRepository,
        )
        val viewModel = WordDetailViewModel(
            appContext = androidContext(),
            word = Word(id = 1L, lemma = "abandon", meanings = listOf("放弃")),
            wordRepository = DetailWordRepository(),
            settingsRepository = settingsRepository,
            phoneticEnrichmentRepository = DetailPhoneticRepository(),
            pronunciationSourceRepository = sourceRepository,
            pronunciationSourceRegistry = registry,
            eventRecorder = NoOpStudyEventRecorder,
        )

        viewModel.refreshPronunciationSourceState()
        val state = viewModel.switchSessionPronunciationSource("native-us")

        assertEquals("native-us", state.selectedPronunciationSourceId)
        assertEquals("美式原生离线包", state.selectedPronunciationSourceLabel)
        assertTrue(state.availablePronunciationSources.single { it.id == "native-us" }.isSelected)
    }
}

private fun androidContext(): Context =
    ApplicationProvider.getApplicationContext()

private class DetailWordRepository : WordRepository {
    private var word = Word(id = 1L, lemma = "abandon", meanings = listOf("放弃"))

    override fun observeWords(query: String): Flow<List<Word>> = MutableStateFlow(listOf(word))

    override suspend fun getWord(wordId: Long): Word? = word.takeIf { it.id == wordId }

    override suspend fun getWords(wordIds: List<Long>): List<Word> = listOfNotNull(word.takeIf { it.id in wordIds })

    override suspend fun getAllWords(): List<Word> = listOf(word)

    override suspend fun insertWord(word: Word): Long = word.id

    override suspend fun updateWord(word: Word) {
        this.word = word
    }

    override suspend fun importWords(words: List<com.yueliangmanle.danci.core.importer.ImportedWord>): List<Long> = emptyList()
}

private class DetailPhoneticRepository : PhoneticEnrichmentRepository {
    override suspend fun insert(job: PhoneticEnrichmentJob): Long = 1L
    override suspend fun update(job: PhoneticEnrichmentJob) = Unit
    override suspend fun getJob(jobId: Long): PhoneticEnrichmentJob? = null
    override suspend fun getLatestJob(scopeType: String, scopeRef: String): PhoneticEnrichmentJob? = null
}

private class DetailSettingsRepository(
    initial: AppSettings,
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    private var sessionPreference = PronunciationSessionPreference()

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

private class DetailSourceRepository(
    private val sources: MutableList<PronunciationSource>,
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

    override suspend fun setDefaultWordSource(sourceId: String) = Unit

    override suspend fun setDefaultLongTextSource(sourceId: String) = Unit

    override suspend fun clearAll() = Unit
}

private class DetailVoicePackRepository : com.yueliangmanle.danci.core.data.VoicePackRepository {
    override suspend fun getAllVoicePacks(): List<com.yueliangmanle.danci.core.model.VoicePack> = emptyList()
    override suspend fun getVoicePack(id: String): com.yueliangmanle.danci.core.model.VoicePack? = null
    override suspend fun activateVoicePack(id: String) = Unit
    override suspend fun upsertVoicePack(voicePack: com.yueliangmanle.danci.core.model.VoicePack) = Unit
    override suspend fun removeVoicePack(id: String) = Unit
    override suspend fun syncManifest(jsonText: String): Int = 0
    override suspend fun refreshCatalog(): Int = 0
    override suspend fun updateVoicePackStatus(id: String, status: String, installDir: String?, installedSizeBytes: Long?) = Unit
    override suspend fun markInstalled(id: String, installDir: String, installedSizeBytes: Long) = Unit
    override fun voicePackRootDir(): java.io.File = java.io.File("/tmp")
}

private fun detailSource(
    id: String,
    type: PronunciationSourceType,
    accent: PronunciationAccent,
    isDefaultForWord: Boolean = false,
    backingVoicePackId: String? = null,
): PronunciationSource =
    PronunciationSource(
        id = id,
        name = when (id) {
            "native-us" -> "美式原生离线包"
            else -> "词典发音（${accent.label}）"
        },
        sourceType = type.storageValue,
        accent = accent.storageValue,
        enabled = true,
        isDefaultForWord = isDefaultForWord,
        backingVoicePackId = backingVoicePackId,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
