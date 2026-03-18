package com.yueliangmanle.danci.core.ai

import org.json.JSONObject

class AiSuggestionParser {
    fun parsePlanAdjustment(text: String): ParsedPlanAdjustment? = runCatching {
        val json = JSONObject(text)
        ParsedPlanAdjustment(
            summary = json.getString("summary"),
            recommendedFocus = json.optJSONArray("recommended_focus").toStringList(),
            suggestedModes = json.optJSONArray("suggested_modes").toStringList(),
            suggestedPace = json.getString("suggested_pace"),
            checkpointAdvice = json.getString("checkpoint_advice"),
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
}

data class ParsedPlanAdjustment(
    val summary: String,
    val recommendedFocus: List<String>,
    val suggestedModes: List<String>,
    val suggestedPace: String,
    val checkpointAdvice: String,
)

data class ParsedWordHelp(
    val title: String,
    val body: String,
    val bullets: List<String>,
)

private fun org.json.JSONArray?.toStringList(): List<String> =
    this?.let { array ->
        List(array.length()) { index ->
            array.optString(index)
        }.filter(String::isNotBlank)
    }.orEmpty()
