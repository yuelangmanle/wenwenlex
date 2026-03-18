package com.yueliangmanle.danci.feature.books

import android.content.Context
import com.yueliangmanle.danci.core.ai.AiProfileResolver
import com.yueliangmanle.danci.core.ai.AiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.PlanSource
import com.yueliangmanle.danci.core.ai.buildAiStrategyCoordinator
import com.yueliangmanle.danci.core.ai.resolveRuntimeSettingsForCapability
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.ImportBatchRepository
import com.yueliangmanle.danci.core.data.PhoneticEnrichmentRepository
import com.yueliangmanle.danci.core.data.SettingsRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildImportBatchRepository
import com.yueliangmanle.danci.core.data.buildPhoneticEnrichmentRepository
import com.yueliangmanle.danci.core.data.buildSettingsRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.model.AiCapability
import com.yueliangmanle.danci.core.model.Book
import com.yueliangmanle.danci.core.model.PHONETIC_SOURCE_AI
import com.yueliangmanle.danci.core.model.PhoneticEnrichmentJob
import com.yueliangmanle.danci.core.model.Word
import com.yueliangmanle.danci.core.model.hasAnyPhonetic
import com.yueliangmanle.danci.core.model.hasCompletePhonetic
import com.yueliangmanle.danci.core.model.needsPhoneticFill
import com.yueliangmanle.danci.core.model.withUpdatedPhonetics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class BookDetailUiState(
    val isLoading: Boolean = false,
    val isFilling: Boolean = false,
    val bookId: String = "",
    val title: String = "",
    val description: String = "",
    val wordCount: Int = 0,
    val sourceLabel: String = "",
    val sourceMeta: String? = null,
    val isActiveBook: Boolean = false,
    val phoneticCoverage: String = "",
    val latestImportSummary: String? = null,
    val words: List<BookWordItemUiState> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

data class BookWordItemUiState(
    val id: Long,
    val word: String,
    val phoneticLabel: String,
    val meaningsLabel: String,
    val phoneticStatusLabel: String,
)

class BookDetailViewModel(
    private val context: Context,
    private val bookRepository: BookRepository,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val importBatchRepository: ImportBatchRepository,
    private val phoneticEnrichmentRepository: PhoneticEnrichmentRepository,
    private val coordinator: AiStrategyCoordinator,
) {
    private var book: Book? = null
    private var words: List<Word> = emptyList()

    suspend fun loadUiState(
        bookId: String,
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): BookDetailUiState {
        book = requireNotNull(bookRepository.getBook(bookId)) { "没有找到对应词书：$bookId" }
        words = bookRepository.getWords(bookId)
        return buildUiState(statusMessage = statusMessage, errorMessage = errorMessage)
    }

    suspend fun setActiveBook(): BookDetailUiState {
        val currentBook = requireNotNull(book) { "Book must be loaded before setting active" }
        settingsRepository.updateActiveBookId(currentBook.id)
        return buildUiState(statusMessage = "已将《${currentBook.title}》设为当前词书。")
    }

    suspend fun fillBookPhonetics(
        overwrite: Boolean,
    ): BookDetailUiState {
        val currentBook = requireNotNull(book) { "Book must be loaded before filling phonetics" }
        val settings = settingsRepository.getSettings()
        val runtimeSettings = resolveRuntimeSettingsForCapability(context, AiCapability.PHONETIC_FILL)
        if (runtimeSettings?.enabled != true || runtimeSettings.apiKey.isNullOrBlank()) {
            return buildUiState(errorMessage = "请先在 AI 设置里配置可用的音标补全 API。")
        }

        val targets = words.filter { word ->
            overwrite || word.needsPhoneticFill()
        }
        if (targets.isEmpty()) {
            return buildUiState(statusMessage = "当前词书没有需要补全的音标。")
        }

        val now = Instant.now()
        val fillMode = if (overwrite) "overwrite_all" else "fill_missing"
        val jobId = phoneticEnrichmentRepository.insert(
            PhoneticEnrichmentJob(
                scopeType = "book",
                scopeRef = currentBook.id,
                profileId = AiProfileResolver().resolveProfileId(settings, AiCapability.PHONETIC_FILL),
                fillMode = fillMode,
                status = "running",
                totalCount = targets.size,
                createdAt = now,
                updatedAt = now,
            ),
        )

        var successCount = 0
        var failedCount = 0
        targets.forEach { word ->
            val result = coordinator.requestPhoneticFill(
                settings = settings,
                runtimeSettings = runtimeSettings,
                word = word,
            )
            val merged = mergePhonetics(word, result, overwrite)
            if (merged == word) {
                failedCount += 1
            } else {
                wordRepository.updateWord(merged)
                successCount += 1
            }
        }

        phoneticEnrichmentRepository.update(
            PhoneticEnrichmentJob(
                id = jobId,
                scopeType = "book",
                scopeRef = currentBook.id,
                profileId = AiProfileResolver().resolveProfileId(settings, AiCapability.PHONETIC_FILL),
                fillMode = fillMode,
                status = if (successCount > 0) "completed" else "failed",
                totalCount = targets.size,
                completedCount = successCount,
                failedCount = failedCount,
                createdAt = now,
                updatedAt = Instant.now(),
            ),
        )

        return loadUiState(
            bookId = currentBook.id,
            statusMessage = if (failedCount == 0) {
                "已完成 ${successCount} 条音标补全。"
            } else {
                "已补全 ${successCount} 条音标，另有 ${failedCount} 条没有拿到更完整结果。"
            },
        )
    }

    private suspend fun buildUiState(
        statusMessage: String? = null,
        errorMessage: String? = null,
    ): BookDetailUiState {
        val currentBook = requireNotNull(book) { "Book must be loaded before building ui state" }
        val settings = settingsRepository.getSettings()
        val latestImport = importBatchRepository.getAll().firstOrNull { it.bookId == currentBook.id }
        val completeCount = words.count(Word::hasCompletePhonetic)
        val partialCount = words.count { !it.hasCompletePhonetic() && it.hasAnyPhonetic() }
        val emptyCount = words.count { !it.hasAnyPhonetic() }
        return BookDetailUiState(
            bookId = currentBook.id,
            title = currentBook.title,
            description = currentBook.description.orEmpty(),
            wordCount = currentBook.wordCount,
            sourceLabel = if (currentBook.sourceType == "builtin") "内置词书" else "导入词书",
            sourceMeta = if (currentBook.sourceType == "builtin") {
                "已内置进本地数据库，可离线使用。"
            } else {
                "来自用户导入，可继续批量补音标或设为当前词书。"
            },
            isActiveBook = settings.activeBookId == currentBook.id,
            phoneticCoverage = "双音标完整 $completeCount · 部分 $partialCount · 空白 $emptyCount",
            latestImportSummary = latestImport?.let {
                "最近导入：${it.fileName} · ${it.parserMode} · ${formatInstant(it.createdAt)}"
            },
            words = words.map(Word::asUiModel),
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }

    private fun mergePhonetics(
        current: Word,
        result: com.yueliangmanle.danci.core.ai.AiPhoneticFillResult,
        overwrite: Boolean,
    ): Word {
        val nextUk = when {
            !result.phoneticUk.isNullOrBlank() -> result.phoneticUk
            overwrite -> current.phoneticUk
            else -> current.phoneticUk
        }
        val nextUs = when {
            !result.phoneticUs.isNullOrBlank() -> result.phoneticUs
            overwrite -> current.phoneticUs
            else -> current.phoneticUs
        }
        val resolvedUk = if (overwrite || current.phoneticUk.isNullOrBlank()) nextUk else current.phoneticUk
        val resolvedUs = if (overwrite || current.phoneticUs.isNullOrBlank()) nextUs else current.phoneticUs
        if (resolvedUk == current.phoneticUk && resolvedUs == current.phoneticUs) {
            return current
        }
        return current.withUpdatedPhonetics(
            phoneticUk = resolvedUk,
            phoneticUs = resolvedUs,
            source = if (result.source == PlanSource.AI) PHONETIC_SOURCE_AI else current.phoneticSource,
            updatedAt = Instant.now(),
        )
    }
}

fun buildBookDetailViewModel(context: Context): BookDetailViewModel =
    BookDetailViewModel(
        context = context.applicationContext,
        bookRepository = buildBookRepository(context),
        wordRepository = buildWordRepository(context),
        settingsRepository = buildSettingsRepository(context),
        importBatchRepository = buildImportBatchRepository(context),
        phoneticEnrichmentRepository = buildPhoneticEnrichmentRepository(context),
        coordinator = buildAiStrategyCoordinator(context),
    )

private fun Word.asUiModel(): BookWordItemUiState =
    BookWordItemUiState(
        id = id,
        word = lemma,
        phoneticLabel = listOfNotNull(
            phoneticUk?.takeIf(String::isNotBlank)?.let { "英：$it" },
            phoneticUs?.takeIf(String::isNotBlank)?.let { "美：$it" },
            phonetic?.takeIf(String::isNotBlank)?.takeIf { phoneticUk.isNullOrBlank() && phoneticUs.isNullOrBlank() }?.let { "音：$it" },
        ).joinToString("  ").ifBlank { "音标待补全" },
        meaningsLabel = meanings.joinToString("；"),
        phoneticStatusLabel = when {
            hasCompletePhonetic() -> "完整"
            hasAnyPhonetic() -> "部分"
            else -> "空白"
        },
    )

private fun formatInstant(instant: Instant): String =
    instant
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
