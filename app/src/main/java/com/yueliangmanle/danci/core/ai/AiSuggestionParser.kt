package com.yueliangmanle.danci.core.ai

import org.json.JSONObject

class AiSuggestionParser {
    fun parsePlanAdjustment(text: String): ParsedPlanAdjustment? = runCatching {
        val json = JSONObject(text)
        ParsedPlanAdjustment(
            summary = json.getString("summary"),
            recommendedFocus = json.optJSONArray("recommended_focus").toStringList(),
            suggestedModes = json.optJSONArray("suggested_modes").toStringList(),
            suggestedPace = json.optNullableString("suggested_pace"),
            checkpointAdvice = json.optNullableString("checkpoint_advice"),
            reasonSummary = json.optNullableString("reason_summary"),
            changeSummary = json.optNullableString("change_summary"),
            abnormalSignals = json.optJSONArray("abnormal_signals").toStringList(),
            executionEffect = json.optNullableString("execution_effect"),
        )
    }.getOrNull()

    fun parseWordHelp(text: String): ParsedWordHelp? = runCatching {
        val json = JSONObject(text)
        ParsedWordHelp(
            title = json.getString("title"),
            body = json.getString("body"),
            bullets = json.optJSONArray("bullets").toStringList(),
        )
    }.getOrNull()

    fun parseImportNormalization(text: String): ParsedImportNormalization? = runCatching {
        val json = JSONObject(text)
        ParsedImportNormalization(
            sheetName = json.optString("sheet_name").ifBlank { null },
            totalRows = json.optInt("total_rows", 0),
            skippedRows = json.optInt("skipped_rows", 0),
            rows = json.optJSONArray("rows").toImportRows(),
        )
    }.getOrNull()

    fun parsePhoneticFill(text: String): ParsedPhoneticFill? = runCatching {
        val json = JSONObject(text)
        ParsedPhoneticFill(
            phoneticUk = json.optString("phonetic_uk").ifBlank { null },
            phoneticUs = json.optString("phonetic_us").ifBlank { null },
        )
    }.getOrNull()
}

data class ParsedPlanAdjustment(
    val summary: String,
    val recommendedFocus: List<String>,
    val suggestedModes: List<String>,
    val suggestedPace: String? = null,
    val checkpointAdvice: String? = null,
    val reasonSummary: String? = null,
    val changeSummary: String? = null,
    val abnormalSignals: List<String> = emptyList(),
    val executionEffect: String? = null,
)

data class ParsedWordHelp(
    val title: String,
    val body: String,
    val bullets: List<String>,
)

data class ParsedImportNormalization(
    val sheetName: String?,
    val totalRows: Int,
    val skippedRows: Int,
    val rows: List<ParsedImportNormalizationRow>,
)

data class ParsedImportNormalizationRow(
    val word: String,
    val meanings: List<String>,
    val phoneticUk: String? = null,
    val phoneticUs: String? = null,
)

data class ParsedPhoneticFill(
    val phoneticUk: String? = null,
    val phoneticUs: String? = null,
)

private fun org.json.JSONArray?.toStringList(): List<String> =
    this?.let { array ->
        List(array.length()) { index ->
            array.optString(index)
        }.filter(String::isNotBlank)
    }.orEmpty()

private fun org.json.JSONArray?.toImportRows(): List<ParsedImportNormalizationRow> =
    this?.let { array ->
        List(array.length()) { index ->
            array.getJSONObject(index).let { row ->
                ParsedImportNormalizationRow(
                    word = row.optString("word").trim(),
                    meanings = row.optJSONArray("meanings").toStringList(),
                    phoneticUk = row.optString("phonetic_uk").ifBlank { null },
                    phoneticUs = row.optString("phonetic_us").ifBlank { null },
                )
            }
        }.filter { it.word.isNotBlank() && it.meanings.isNotEmpty() }
    }.orEmpty()

private fun JSONObject.optNullableString(key: String): String? =
    optString(key).ifBlank { null }
