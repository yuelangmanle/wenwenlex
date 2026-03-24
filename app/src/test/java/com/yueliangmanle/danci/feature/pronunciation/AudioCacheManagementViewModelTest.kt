package com.yueliangmanle.danci.feature.pronunciation

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.data.AudioCacheBucket
import com.yueliangmanle.danci.core.data.AudioCacheEntry
import com.yueliangmanle.danci.core.data.AudioCacheFilter
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordAudioRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.importer.ImportedWord
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.DictionaryAudioCandidate
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.WordAudioAsset
import java.io.InputStream
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioCacheManagementViewModelTest {
    @Test
    fun loadUiState_groupsAssetsBySourceBucket() = runTest {
        val viewModel = AudioCacheManagementViewModel(
            settingsRepository = FakeAudioCacheSettingsRepository(),
            wordAudioRepository = FakeAudioCacheWordAudioRepository(
                buckets = listOf(
                    AudioCacheBucket(
                        sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue,
                        count = 2,
                        totalBytes = 120,
                    ),
                    AudioCacheBucket(
                        sourceType = PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue,
                        count = 1,
                        totalBytes = 80,
                    ),
                    AudioCacheBucket(
                        sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue,
                        count = 1,
                        totalBytes = 100,
                    ),
                ),
            ),
            bookRepository = FakeAudioCacheBookRepository(),
            wordRepository = FakeAudioCacheWordRepository(),
        )

        val state = viewModel.loadUiState()

        assertEquals(3, state.buckets.size)
    }

    @Test
    fun applyFilter_scopesAssetsByBookAndKeyword() = runTest {
        val words = listOf(
            Word(id = 1L, lemma = "abandon", meanings = listOf("放弃")),
            Word(id = 2L, lemma = "ability", meanings = listOf("能力")),
            Word(id = 3L, lemma = "zebra", meanings = listOf("斑马")),
        )
        val viewModel = AudioCacheManagementViewModel(
            settingsRepository = FakeAudioCacheSettingsRepository(),
            wordAudioRepository = FakeAudioCacheWordAudioRepository(
                entries = listOf(
                    entry(wordId = 1L, sourceType = PlaybackSource.DICTIONARY_CACHE.storageValue),
                    entry(wordId = 2L, sourceType = PlaybackSource.ONLINE_PREBUILT_CACHE.storageValue),
                    entry(wordId = 3L, sourceType = PlaybackSource.OFFLINE_NATIVE_GENERATED.storageValue),
                ),
            ),
            bookRepository = FakeAudioCacheBookRepository(
                books = listOf(
                    Book(id = "cet4", title = "四级", wordCount = 2),
                    Book(id = "ielts", title = "雅思", wordCount = 1),
                ),
                wordsByBookId = mapOf(
                    "cet4" to words.take(2),
                    "ielts" to listOf(words[2]),
                ),
            ),
            wordRepository = FakeAudioCacheWordRepository(words),
        )

        val state = viewModel.loadUiState(
            filter = AudioCacheFilter(bookId = "cet4", query = "abandon"),
        )

        assertEquals(listOf("abandon"), state.items.map { it.word })
    }
}

private fun entry(
    wordId: Long,
    sourceType: String,
): AudioCacheEntry =
    AudioCacheEntry(
        assetId = wordId,
        wordId = wordId,
        accent = PronunciationAccent.UK.storageValue,
        sourceType = sourceType,
        sourceLabel = PlaybackSource.fromStorageValue(sourceType).label,
        localPath = "/tmp/$wordId.mp3",
        sizeBytes = 32L,
        fetchedAt = Instant.parse("2026-03-19T12:00:00Z"),
        lastPlayedAt = null,
        lastError = null,
    )

private class FakeAudioCacheSettingsRepository(
    initial: AppSettings = AppSettings(audioCacheLimitMb = 300),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<AppSettings> = state

    override suspend fun getSettings(): AppSettings = state.value

    override suspend fun updateDailyGoal(dailyGoal: Int) = Unit
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
    override suspend fun updateAudioCacheLimitMb(limitMb: Int) {
        state.value = state.value.copy(audioCacheLimitMb = limitMb)
    }
    override suspend fun updateActiveVoicePackId(voicePackId: String?) = Unit
    override suspend fun updateFallbackToSystemTts(enabled: Boolean) = Unit
    override suspend fun updatePreferOfflineForLongText(enabled: Boolean) = Unit
    override suspend fun updateReminderEnabled(enabled: Boolean) = Unit
    override suspend fun updateReminderTime(hour: Int, minute: Int) = Unit
}

private class FakeAudioCacheWordAudioRepository(
    private val buckets: List<AudioCacheBucket> = emptyList(),
    private val entries: List<AudioCacheEntry> = emptyList(),
) : WordAudioRepository {
    override suspend fun findCachedAsset(wordId: Long, accent: PronunciationAccent): WordAudioAsset? = null
    override suspend fun isRemoteLookupCoolingDown(wordId: Long, accent: PronunciationAccent): Boolean = false
    override suspend fun cacheDictionaryAudio(wordId: Long, candidate: DictionaryAudioCandidate): WordAudioAsset? = null
    override suspend fun markRemoteLookupFailure(wordId: Long, accent: PronunciationAccent, errorMessage: String) = Unit
    override suspend fun markPlayed(asset: WordAudioAsset) = Unit
    override suspend fun clearDictionaryCache(): Int = 0
    override suspend fun cacheSizeBytes(): Long = entries.sumOf(AudioCacheEntry::sizeBytes)
    override suspend fun summarizeBySource(): List<AudioCacheBucket> = buckets
    override suspend fun queryAssets(filter: AudioCacheFilter): List<AudioCacheEntry> = entries
}

private class FakeAudioCacheBookRepository(
    private val books: List<Book> = listOf(Book(id = "cet4", title = "四级", wordCount = 2)),
    private val wordsByBookId: Map<String, List<Word>> = emptyMap(),
) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = flowOf(books)
    override fun observeWords(bookId: String): Flow<List<Word>> = flowOf(wordsByBookId[bookId].orEmpty())
    override suspend fun getBook(bookId: String): Book? = books.firstOrNull { it.id == bookId }
    override suspend fun getAllBooks(): List<Book> = books
    override suspend fun getWords(bookId: String): List<Word> = wordsByBookId[bookId].orEmpty()
    override suspend fun countWords(bookId: String): Int = wordsByBookId[bookId]?.size ?: 0
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
    override fun loadBuiltInCatalog(inputStream: InputStream) = emptyList<com.yueliangmanle.danci.core.data.BuiltInBookCatalogItem>()
    override suspend fun importBook(book: com.yueliangmanle.danci.core.importer.ImportedBook, wordIds: List<Long>): Book =
        Book(
            id = book.metadata.id,
            title = book.metadata.title,
            description = book.metadata.description,
            language = book.metadata.language,
            category = book.metadata.category,
            sourceType = book.metadata.sourceType,
            wordCount = wordIds.size,
        )
}

private class FakeAudioCacheWordRepository(
    private val words: List<Word> = emptyList(),
) : WordRepository {
    override fun observeWords(query: String): Flow<List<Word>> = flowOf(words)
    override suspend fun getWord(wordId: Long): Word? = words.firstOrNull { it.id == wordId }
    override suspend fun getWords(wordIds: List<Long>): List<Word> = words.filter { it.id in wordIds.toSet() }
    override suspend fun getAllWords(): List<Word> = words
    override suspend fun insertWord(word: Word): Long = word.id
    override suspend fun updateWord(word: Word) = Unit
    override suspend fun importWords(words: List<ImportedWord>): List<Long> = emptyList()
}
