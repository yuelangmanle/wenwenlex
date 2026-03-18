package com.yueliangmanle.danci.feature.books

import android.content.Context
import com.yueliangmanle.danci.core.ai.resolveRuntimeSettingsForCapability
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.ImportBatchRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildImportBatchRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.importer.AiImportNormalizer
import com.yueliangmanle.danci.core.importer.ImportPreview
import com.yueliangmanle.danci.core.importer.ImportPreviewRow
import com.yueliangmanle.danci.core.importer.XlsxBookImporter
import com.yueliangmanle.danci.core.importer.XlsxSheetData
import com.yueliangmanle.danci.core.importer.XlsxXmlTableReader
import com.yueliangmanle.danci.core.model.AiCapability
import com.yueliangmanle.danci.core.model.ImportBatch
import java.time.Instant
import java.util.Locale

data class BookImportUiState(
    val isLoading: Boolean = false,
    val isAiNormalizing: Boolean = false,
    val isImporting: Boolean = false,
    val fileName: String? = null,
    val bookTitle: String = "",
    val preview: ImportPreview? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val importedBookId: String? = null,
)

class BookImportViewModel(
    private val context: Context,
    private val wordRepository: WordRepository,
    private val bookRepository: BookRepository,
    private val importBatchRepository: ImportBatchRepository,
    private val tableReader: XlsxXmlTableReader = XlsxXmlTableReader(),
    private val xlsxBookImporter: XlsxBookImporter = XlsxBookImporter(),
    private val aiImportNormalizer: AiImportNormalizer = AiImportNormalizer(),
) {
    private var workbookSheets: List<XlsxSheetData> = emptyList()
    private var currentPreview: ImportPreview? = null
    private var currentFileName: String? = null

    fun updateBookTitle(
        current: BookImportUiState,
        title: String,
    ): BookImportUiState = current.copy(bookTitle = title)

    suspend fun loadFromBytes(
        fileName: String,
        bytes: ByteArray,
    ): BookImportUiState = runCatching {
        workbookSheets = tableReader.readWorkbook(bytes.inputStream())
        currentPreview = xlsxBookImporter.preview(workbookSheets.firstOrNull { it.rows.isNotEmpty() } ?: workbookSheets.first())
        currentFileName = fileName
        BookImportUiState(
            fileName = fileName,
            bookTitle = fileName.substringBeforeLast(".").ifBlank { "Excel 导入词书" },
            preview = currentPreview,
            statusMessage = "已完成本地解析，可以先检查预览再导入。",
        )
    }.getOrElse { error ->
        BookImportUiState(
            fileName = fileName,
            errorMessage = error.message ?: "文件解析失败，请确认它是 .xlsx 文件。",
        )
    }

    suspend fun normalizeWithAi(
        current: BookImportUiState,
    ): BookImportUiState {
        if (workbookSheets.isEmpty()) {
            return current.copy(errorMessage = "请先选择 Excel 文件。")
        }
        val runtimeSettings = resolveRuntimeSettingsForCapability(context, AiCapability.WORD_HELP)
        val preview = aiImportNormalizer.normalize(workbookSheets, runtimeSettings)
        currentPreview = preview
        return current.copy(
            isAiNormalizing = false,
            preview = preview,
            statusMessage = if (runtimeSettings?.enabled == true && !runtimeSettings.apiKey.isNullOrBlank()) {
                "AI 已按当前表格结构重排预览，请确认后导入。"
            } else {
                "当前没有可用 AI API，已回退到本地启发式适配。"
            },
            errorMessage = null,
        )
    }

    suspend fun importCurrent(
        current: BookImportUiState,
    ): BookImportUiState {
        val preview = currentPreview ?: return current.copy(errorMessage = "还没有可导入的预览数据。")
        val title = current.bookTitle.trim().ifBlank {
            current.fileName?.substringBeforeLast(".")?.ifBlank { "Excel 导入词书" } ?: "Excel 导入词书"
        }
        val bookId = buildImportedBookId(title)
        val importedBook = xlsxBookImporter.toImportedBook(
            preview = preview,
            title = title,
            id = bookId,
        )
        return runCatching {
            val wordIds = wordRepository.importWords(importedBook.words)
            val book = bookRepository.importBook(importedBook, wordIds)
            importBatchRepository.insert(
                ImportBatch(
                    bookId = book.id,
                    fileName = currentFileName ?: title,
                    sheetName = preview.sheetName,
                    parserMode = preview.parserMode,
                    totalRows = preview.totalRows,
                    importedRows = preview.rows.size,
                    skippedRows = preview.skippedRows,
                    aiNormalizedCount = preview.aiNormalizedCount,
                    aiCompletedCount = preview.aiCompletedCount,
                    createdAt = Instant.now(),
                ),
            )
            current.copy(
                isImporting = false,
                importedBookId = book.id,
                statusMessage = "已导入 ${wordIds.size} 个词，接下来可以进入词书详情设为当前词书或批量补音标。",
                errorMessage = null,
            )
        }.getOrElse { error ->
            current.copy(
                isImporting = false,
                errorMessage = error.message ?: "导入失败，请检查文件内容。",
            )
        }
    }
}

fun buildBookImportViewModel(context: Context): BookImportViewModel =
    BookImportViewModel(
        context = context.applicationContext,
        wordRepository = buildWordRepository(context),
        bookRepository = buildBookRepository(context),
        importBatchRepository = buildImportBatchRepository(context),
    )

fun defaultPreviewRows(preview: ImportPreview?): List<ImportPreviewRow> =
    preview?.rows?.take(20).orEmpty()

private fun buildImportedBookId(title: String): String {
    val slug = title
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .takeIf(String::isNotBlank)
        ?: "book"
    return "imported-$slug-${Instant.now().epochSecond}"
}
