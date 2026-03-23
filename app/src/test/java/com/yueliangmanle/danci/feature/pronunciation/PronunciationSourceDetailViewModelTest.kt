package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AiProfileRepository
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.PronunciationSourceRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.importer.ImportedBook
import com.yueliangmanle.danci.core.model.AI_PROVIDER_TYPE_MIMO_TTS
import com.yueliangmanle.danci.core.model.AiProviderProfile
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.MIMO_TTS_BASE_URL
import com.yueliangmanle.danci.core.model.MIMO_TTS_MODEL
import com.yueliangmanle.danci.core.model.PronunciationSource
import com.yueliangmanle.danci.core.model.PronunciationSourcePreset
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationCoordinator
import com.yueliangmanle.danci.core.pronunciation.AudioGenerationWorkScheduler
import com.yueliangmanle.danci.core.pronunciation.ApiHealthChecker
import com.yueliangmanle.danci.core.pronunciation.FakeCloudTtsTransport
import com.yueliangmanle.danci.core.pronunciation.MiMoTtsProvider
import com.yueliangmanle.danci.core.security.AiCredentialStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
            bookRepository = FakeDetailBookRepository(),
            wordRepository = FakeDetailWordRepository(),
            apiHealthChecker = ApiHealthChecker(
                miMoTtsProvider = MiMoTtsProvider(transport = FakeCloudTtsTransport()),
            ),
            audioGenerationCoordinator = buildDetailCoordinator(sourceRepository),
        )

        val state = viewModel.loadUiState("cloud-mimo")

        assertEquals("MiMo 云端发音", state.sourceTitle)
        assertEquals("profile-mimo", state.selectedProfileId)
        assertEquals("preset-calm", state.selectedPresetId)
        assertEquals(2, state.profileOptions.size)
        assertTrue(state.canCheckApi)
        assertTrue(state.generationSupported)
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
            bookRepository = FakeDetailBookRepository(),
            wordRepository = FakeDetailWordRepository(),
            apiHealthChecker = ApiHealthChecker(
                miMoTtsProvider = MiMoTtsProvider(transport = FakeCloudTtsTransport()),
            ),
            audioGenerationCoordinator = buildDetailCoordinator(sourceRepository),
        )

        val state = viewModel.bindProfile("cloud-mimo", "profile-mimo-2")

        assertEquals("profile-mimo-2", state.selectedProfileId)
        assertEquals("备用 MiMo", state.profileOptions.single { it.id == "profile-mimo-2" }.title)
        assertEquals("profile-mimo-2", sourceRepository.sources.single().providerProfileId)
    }

    @Test
    fun enqueueSingleWordGeneration_createsBackgroundTaskWhenWordExists() = runTest {
        val sourceRepository = FakeDetailSourceRepository(
            mutableListOf(
                cloudSource(providerProfileId = "profile-mimo"),
            ),
        )
        val taskRepository = FakeDetailAudioGenerationRepository()
        val coordinator = buildDetailCoordinator(
            sourceRepository = sourceRepository,
            taskRepository = taskRepository,
        )
        val viewModel = PronunciationSourceDetailViewModel(
            pronunciationSourceRepository = sourceRepository,
            aiProfileRepository = FakeDetailAiProfileRepository(listOf(mimoProfile(id = "profile-mimo"))),
            bookRepository = FakeDetailBookRepository(),
            wordRepository = FakeDetailWordRepository(
                listOf(
                    Word(id = 10L, lemma = "abandon"),
                ),
            ),
            credentialStore = FakeDetailCredentialStore(
                mapOf("profile-mimo" to "sk-live"),
            ),
            apiHealthChecker = ApiHealthChecker(
                miMoTtsProvider = MiMoTtsProvider(transport = FakeCloudTtsTransport()),
            ),
            audioGenerationCoordinator = coordinator,
        )
        val current = viewModel.loadUiState("cloud-mimo")

        val state = viewModel.enqueueSingleWordGeneration("cloud-mimo", current)

        assertTrue(state.statusMessage?.contains("abandon") == true)
        assertEquals(1, taskRepository.tasks.size)
        assertEquals("word", taskRepository.tasks.single().scopeType)
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

private class FakeDetailBookRepository(
    private val books: List<Book> = listOf(
        Book(id = "cet4", title = "CET4", wordCount = 100),
    ),
) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = emptyFlow()
    override fun observeWords(bookId: String): Flow<List<Word>> = emptyFlow()
    override suspend fun getBook(bookId: String): Book? = books.firstOrNull { it.id == bookId }
    override suspend fun getAllBooks(): List<Book> = books
    override suspend fun getWords(bookId: String): List<Word> = emptyList()
    override suspend fun countWords(bookId: String): Int = books.firstOrNull { it.id == bookId }?.wordCount ?: 0
    override suspend fun upsertBook(book: Book) = Unit
    override suspend fun addWordToBook(
        bookId: String,
        wordId: Long,
        chapter: String?,
        sortOrder: Int,
        tags: List<String>,
        note: String?,
    ) = Unit

    override suspend fun clearBookWordLinks(bookId: String) = Unit

    override fun loadBuiltInCatalog(inputStream: java.io.InputStream): List<com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem> =
        emptyList()

    override suspend fun importBook(book: ImportedBook, wordIds: List<Long>): Book =
        Book(id = book.metadata.id, title = book.metadata.title, wordCount = wordIds.size)
}

private class FakeDetailWordRepository(
    private val words: List<Word> = emptyList(),
) : WordRepository {
    override fun observeWords(query: String): Flow<List<Word>> = emptyFlow()
    override suspend fun getWord(wordId: Long): Word? = words.firstOrNull { it.id == wordId }
    override suspend fun getWords(wordIds: List<Long>): List<Word> = words.filter { it.id in wordIds }
    override suspend fun getAllWords(): List<Word> = words
    override suspend fun insertWord(word: Word): Long = word.id
    override suspend fun updateWord(word: Word) = Unit
    override suspend fun importWords(words: List<com.yueliangmanle.danci.core.importer.ImportedWord>): List<Long> =
        emptyList()
}

private class FakeDetailAudioGenerationRepository : com.yueliangmanle.danci.core.data.AudioGenerationRepository {
    val tasks = mutableListOf<com.yueliangmanle.danci.core.model.AudioGenerationTask>()

    override suspend fun getAllTasks(): List<com.yueliangmanle.danci.core.model.AudioGenerationTask> = tasks.toList()

    override suspend fun getTask(taskId: String): com.yueliangmanle.danci.core.model.AudioGenerationTask? =
        tasks.firstOrNull { it.id == taskId }

    override suspend fun upsertTasks(tasks: List<com.yueliangmanle.danci.core.model.AudioGenerationTask>) {
        tasks.forEach { task ->
            val index = this.tasks.indexOfFirst { it.id == task.id }
            if (index >= 0) {
                this.tasks[index] = task
            } else {
                this.tasks += task
            }
        }
    }

    override suspend fun clearAll() = Unit
}

private fun buildDetailCoordinator(
    sourceRepository: FakeDetailSourceRepository,
    taskRepository: FakeDetailAudioGenerationRepository = FakeDetailAudioGenerationRepository(),
): AudioGenerationCoordinator =
    AudioGenerationCoordinator(
        audioGenerationRepository = taskRepository,
        pronunciationSourceRepository = sourceRepository,
        wordRepository = FakeDetailWordRepository(
            listOf(
                Word(id = 10L, lemma = "abandon"),
            ),
        ),
        bookRepository = FakeDetailBookRepository(),
        scheduler = object : AudioGenerationWorkScheduler {
            override suspend fun enqueue(taskId: String, batchSize: Int, requiresNetwork: Boolean) = Unit
        },
        idGenerator = { "detail-task-1" },
        nowProvider = { Instant.EPOCH },
    )
