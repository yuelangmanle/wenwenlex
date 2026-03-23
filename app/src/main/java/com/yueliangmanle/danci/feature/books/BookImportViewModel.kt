package com.yueliangmanle.danci.feature.books

import android.content.Context
import com.yueliangmanle.danci.core.ai.AiRuntimeSettings
import com.yueliangmanle.danci.core.ai.resolveRuntimeSettingsForCapability
import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.ImportBatchRepository
import com.yueliangmanle.danci.core.data.WordRepository
import com.yueliangmanle.danci.core.data.buildBookRepository
import com.yueliangmanle.danci.core.data.buildImportBatchRepository
import com.yueliangmanle.danci.core.data.buildWordRepository
import com.yueliangmanle.danci.core.importer.AiImportNormalizer
import com.yueliangmanle.danci.core.importer.ImportDiagnosis
import com.yueliangmanle.danci.core.importer.ImportPreview
import com.yueliangmanle.danci.core.importer.ImportPreviewRow
import com.yueliangmanle.danci.core.importer.ImportRepairCoordinator
import com.yueliangmanle.danci.core.importer.XlsxBookImporter
import com.yueliangmanle.danci.core.importer.XlsxSheetData
import com.yueliangmanle.danci.core.importer.XlsxXmlTableReader
import com.yueliangmanle.danci.core.importer.toSummary
import com.yueliangmanle.danci.core.model.AiCapability
import com.yueliangmanle.danci.core.model.ImportBatch
import com.yueliangmanle.danci.core.model.ImportDiagnosisSnapshot
import java.time.Instant
import java.util.Locale

data class BookImportUiState(
    val isLoading: Boolean = false,
    val isAiNormalizing: Boolean = false,
    val isImporting: Boolean = false,
    val fileName: String? = null,
    val bookTitle: String = "",
    val preview: ImportPreview? = null,
    val diagnosisSummary: String? = null,
    val minorIssueCount: Int = 0,
    val severeIssueCount: Int = 0,
    val canRunAiRepair: Boolean = false,
    val autoRepairSummary: String? = null,
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
    private val workbookLoader: (ByteArray) -> List<XlsxSheetData> = { bytes ->
        tableReader.readWorkbook(bytes.inputStream())
    },
    private val runtimeSettingsResolver: suspend (Context, AiCapability) -> AiRuntimeSettings? = ::resolveRuntimeSettingsForCapability,
    private val repairCoordinator: ImportRepairCoordinator = ImportRepairCoordinator(
        aiNormalize = { sheets, runtimeSettings ->
            aiImportNormalizer.normalize(sheets, runtimeSettings)
        },
    ),
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    private var workbookSheets: List<XlsxSheetData> = emptyList()
    private var currentPreview: ImportPreview? = null
    private var currentFileName: String? = null
    private var currentDiagnosis: ImportDiagnosis? = null

    fun updateBookTitle(
        current: BookImportUiState,
        title: String,
    ): BookImportUiState = current.copy(bookTitle = title)

    suspend fun loadFromBytes(
        fileName: String,
        bytes: ByteArray,
    ): BookImportUiState = runCatching {
        workbookSheets = workbookLoader(bytes)
        require(workbookSheets.isNotEmpty()) { "Excel 文件中没有可读取的工作表" }
        val repairResult = repairCoordinator.repair(
            workbook = workbookSheets,
            allowAi = false,
            runtimeSettings = null,
        )
        currentPreview = repairResult.preview
        currentDiagnosis = repairResult.diagnosis
        currentFileName = fileName
        BookImportUiState(
            fileName = fileName,
            bookTitle = fileName.substringBeforeLast(".").ifBlank { "Excel 导入词书" },
            preview = repairResult.preview,
            diagnosisSummary = repairResult.diagnosis.toSummary(),
            minorIssueCount = repairResult.diagnosis.autoFixableCount,
            severeIssueCount = repairResult.diagnosis.severeIssueCount,
            canRunAiRepair = repairResult.diagnosis.requiresAiDecision,
            autoRepairSummary = repairResult.autoRepairSummary,
            statusMessage = when {
                repairResult.diagnosis.requiresAiDecision && repairResult.diagnosis.autoFixableCount > 0 ->
                    "已完成本地诊断并修复轻微问题，表格里还有高风险结构问题，建议先点“AI 修复严重问题”。"
                repairResult.diagnosis.requiresAiDecision ->
                    "已完成本地诊断，表格里还有高风险结构问题，建议先点“AI 修复严重问题”。"
                repairResult.diagnosis.autoFixableCount > 0 ->
                    "已完成本地诊断并自动修复轻微问题，可以先检查预览再导入。"
                else -> "已完成本地解析，可以先检查预览再导入。"
            },
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
        val runtimeSettings = runtimeSettingsResolver(context, AiCapability.WORD_HELP)
        val repairResult = repairCoordinator.repair(
            workbook = workbookSheets,
            allowAi = true,
            runtimeSettings = runtimeSettings,
        )
        currentPreview = repairResult.preview
        currentDiagnosis = repairResult.diagnosis
        return current.copy(
            isAiNormalizing = false,
            preview = repairResult.preview,
            diagnosisSummary = if (repairResult.aiUsed) {
                "原表检测到 ${repairResult.diagnosis.severeIssueCount} 处高风险结构问题，已使用 AI 修复并生成新预览。"
            } else {
                repairResult.diagnosis.toSummary()
            },
            minorIssueCount = repairResult.diagnosis.autoFixableCount,
            severeIssueCount = repairResult.diagnosis.severeIssueCount,
            canRunAiRepair = false,
            autoRepairSummary = repairResult.autoRepairSummary,
            statusMessage = if (runtimeSettings?.enabled == true && !runtimeSettings.apiKey.isNullOrBlank()) {
                "AI 已修复高风险结构问题并重排预览，请确认后导入。"
            } else {
                "当前没有可用 AI API，已保留本地自动修复后的预览。"
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
        val bookId = buildImportedBookId(title, nowProvider())
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
                    diagnosisSnapshot = currentDiagnosis?.toSnapshot(
                        preview = preview,
                    ),
                    parserMode = preview.parserMode,
                    totalRows = preview.totalRows,
                    importedRows = preview.rows.size,
                    skippedRows = preview.skippedRows,
                    aiNormalizedCount = preview.aiNormalizedCount,
                    aiCompletedCount = preview.aiCompletedCount,
                    createdAt = nowProvider(),
                ),
            )
            current.copy(
                isImporting = false,
                importedBookId = book.id,
                statusMessage = "已导入 ${wordIds.size} 个词，诊断摘要已记录；接下来可以去词书详情发起音标、近义词、反义词、例句和词形变化补强。",
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

private fun buildImportedBookId(
    title: String,
    now: Instant,
): String {
    val slug = title
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .takeIf(String::isNotBlank)
        ?: "book"
    return "imported-$slug-${now.epochSecond}"
}

private fun ImportDiagnosis.toSnapshot(
    preview: ImportPreview,
): ImportDiagnosisSnapshot =
    ImportDiagnosisSnapshot(
        inferredStructureConfidence = inferredStructureConfidence,
        autoFixCount = autoFixableCount,
        aiFixCount = if (preview.parserMode.startsWith("ai")) preview.aiNormalizedCount else 0,
        highRiskIssueCount = severeIssueCount,
        finalImportedRowCount = preview.rows.size,
    )
