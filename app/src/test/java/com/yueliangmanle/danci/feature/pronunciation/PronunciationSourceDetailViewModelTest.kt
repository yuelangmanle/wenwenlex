package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AiProfileRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.model.AI_PROVIDER_TYPE_MIMO_TTS
import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.model.MIMO_TTS_BASE_URL
import com.yueliangmanle.danci.core.model.MIMO_TTS_MODEL
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourcePreset
import com.yueliangmanle.danci.core.pronunciation.ApiHealthChecker
import com.yueliangmanle.danci.core.pronunciation.FakeCloudTtsTransport
import com.yueliangmanle.danci.core.pronunciation.MiMoTtsProvider
import com.yueliangmanle.danci.core.security.AiCredentialStore
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationSourceDetailViewModelTest {
    @Test
    fun loadUiState_mapsCompatibleProfilesAndDefaultPreset() = runTest {
        val sourceRepository = FakeDetailSourceRepository(
            mutableListOf(
                cloudSource(
                    providerProfileId = "profile-mimo",
                ),
            ),
        )
        val viewModel = PronunciationSourceDetailViewModel(
            pronunciationSourceRepository = sourceRepository,
            aiProfileRepository = FakeDetailAiProfileRepository(
                listOf(
                    mimoProfile(id = "profile-mimo"),
                    mimoProfile(id = "profile-mimo-2", name = "备用 MiMo"),
                ),
            ),
            credentialStore = FakeDetailCredentialStore(
                mapOf("profile-mimo" to "sk-live"),
            ),
            apiHealthChecker = ApiHealthChecker(
                miMoTtsProvider = MiMoTtsProvider(transport = FakeCloudTtsTransport()),
            ),
        )

        val state = viewModel.loadUiState("cloud-mimo")

        assertEquals("MiMo 云端发音", state.sourceTitle)
        assertEquals("profile-mimo", state.selectedProfileId)
        assertEquals("preset-calm", state.selectedPresetId)
        assertEquals(2, state.profileOptions.size)
        assertTrue(state.canCheckApi)
    }

    @Test
    fun bindProfile_updatesSourceAndRefreshesState() = runTest {
        val sourceRepository = FakeDetailSourceRepository(
            mutableListOf(
                cloudSource(providerProfileId = "profile-mimo"),
            ),
        )
        val viewModel = PronunciationSourceDetailViewModel(
            pronunciationSourceRepository = sourceRepository,
            aiProfileRepository = FakeDetailAiProfileRepository(
                listOf(
                    mimoProfile(id = "profile-mimo"),
                    mimoProfile(id = "profile-mimo-2", name = "备用 MiMo"),
                ),
            ),
            credentialStore = FakeDetailCredentialStore(
                mapOf("profile-mimo-2" to "sk-live-2"),
            ),
            apiHealthChecker = ApiHealthChecker(
                miMoTtsProvider = MiMoTtsProvider(transport = FakeCloudTtsTransport()),
            ),
        )

        val state = viewModel.bindProfile("cloud-mimo", "profile-mimo-2")

        assertEquals("profile-mimo-2", state.selectedProfileId)
        assertEquals("备用 MiMo", state.profileOptions.single { it.id == "profile-mimo-2" }.title)
        assertEquals("profile-mimo-2", sourceRepository.sources.single().providerProfileId)
    }
}

private fun cloudSource(
    providerProfileId: String? = null,
): PronunciationSource =
    PronunciationSource(
        id = "cloud-mimo",
        name = "MiMo 云端发音",
        sourceType = "cloud_tts",
        accent = "auto",
        enabled = true,
        isDefaultForWord = false,
        isDefaultForLongText = false,
        providerProfileId = providerProfileId,
        presets = listOf(
            PronunciationSourcePreset(
                sourceId = "cloud-mimo",
                presetId = "preset-calm",
                displayName = "平静讲解",
                voice = "default_en",
                styleTemplate = "Slow down Calm",
                advancedStyleEnabled = false,
                isDefaultPreset = true,
            ),
            PronunciationSourcePreset(
                sourceId = "cloud-mimo",
                presetId = "preset-happy",
                displayName = "轻快鼓励",
                voice = "default_en",
                styleTemplate = "Happy",
                advancedStyleEnabled = false,
                isDefaultPreset = false,
            ),
        ),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

private fun mimoProfile(
    id: String,
    name: String = "MiMo TTS",
): AiProviderProfile =
    AiProviderProfile(
        id = id,
        name = name,
        providerType = AI_PROVIDER_TYPE_MIMO_TTS,
        baseUrl = MIMO_TTS_BASE_URL,
        model = MIMO_TTS_MODEL,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

private class FakeDetailSourceRepository(
    val sources: MutableList<PronunciationSource>,
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

private class FakeDetailAiProfileRepository(
    private val profiles: List<AiProviderProfile>,
) : AiProfileRepository {
    override suspend fun getProfiles(): List<AiProviderProfile> = profiles

    override suspend fun getProfile(profileId: String): AiProviderProfile? =
        profiles.firstOrNull { it.id == profileId }

    override suspend fun saveProfile(profile: AiProviderProfile) = Unit

    override suspend fun deleteProfile(profileId: String) = Unit
}

private class FakeDetailCredentialStore(
    private val keysByProfileId: Map<String, String>,
) : AiCredentialStore {
    override suspend fun saveApiKey(key: String) = Unit
    override suspend fun readApiKey(): String? = null
    override suspend fun clearApiKey() = Unit
    override suspend fun saveApiKey(profileId: String, key: String) = Unit
    override suspend fun readApiKey(profileId: String): String? = keysByProfileId[profileId]
    override suspend fun clearApiKey(profileId: String) = Unit
}
