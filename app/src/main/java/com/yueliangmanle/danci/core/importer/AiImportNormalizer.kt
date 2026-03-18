package com.yueliangmanle.danci.core.importer

import com.yueliangmanle.danci.core.ai.AiClient
import com.yueliangmanle.danci.core.ai.AiPromptFactory
import com.yueliangmanle.danci.core.ai.AiRuntimeSettings
import com.yueliangmanle.danci.core.ai.AiSuggestionParser
import com.yueliangmanle.danci.core.ai.AiTextRequest
import com.yueliangmanle.danci.core.ai.ResponsesApiAiClient

class AiImportNormalizer(
    private val client: AiClient = ResponsesApiAiClient(),
    private val promptFactory: AiPromptFactory = AiPromptFactory(),
    private val parser: AiSuggestionParser = AiSuggestionParser(),
    private val importer: XlsxBookImporter = XlsxBookImporter(),
) {
    suspend fun normalize(
        workbookSheets: List<XlsxSheetData>,
        runtimeSettings: AiRuntimeSettings?,
    ): ImportPreview {
        val localFallback = bestEffortLocal(workbookSheets)
        if (runtimeSettings?.enabled != true || runtimeSettings.apiKey.isNullOrBlank()) {
            return localFallback
        }

        val prompt = promptFactory.buildImportNormalizationPrompt(workbookSheets)
        val parsed = runCatching {
            client.generate(
                AiTextRequest(
                    runtimeSettings = runtimeSettings,
                    instructions = prompt.instructions,
                    input = prompt.input,
                    responseFormat = prompt.responseFormat,
                ),
            )
        }.mapCatching { response ->
            parser.parseImportNormalization(response.text)
        }.getOrNull()

        return parsed?.let { normalized ->
            ImportPreview(
                sheetName = normalized.sheetName ?: localFallback.sheetName,
                rows = normalized.rows.map { row ->
                    ImportPreviewRow(
                        word = row.word,
                        meanings = row.meanings,
                        phoneticUk = row.phoneticUk,
                        phoneticUs = row.phoneticUs,
                        sourceLabel = "AI 适配",
                    )
                },
                totalRows = normalized.totalRows.takeIf { it > 0 } ?: localFallback.totalRows,
                skippedRows = normalized.skippedRows.takeIf { it >= 0 } ?: localFallback.skippedRows,
                parserMode = "ai_assisted",
                aiNormalizedCount = normalized.rows.size,
                aiCompletedCount = normalized.rows.count { it.meanings.isNotEmpty() || !it.phoneticUk.isNullOrBlank() || !it.phoneticUs.isNullOrBlank() },
                warningMessage = null,
            )
        } ?: localFallback
    }

    fun bestEffortLocal(workbookSheets: List<XlsxSheetData>): ImportPreview {
        val bestSheet = workbookSheets
            .map(importer::preview)
            .maxByOrNull { it.rows.size }
            ?: ImportPreview(
                sheetName = "Sheet1",
                rows = emptyList(),
                totalRows = 0,
                skippedRows = 0,
                warningMessage = "没有可识别的数据。",
            )

        return bestSheet.copy(
            parserMode = "ai_assisted",
            warningMessage = bestSheet.warningMessage ?: "已用本地启发式适配结构，请检查预览后再导入。",
        )
    }
}
