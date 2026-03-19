package com.yueliangmanle.danci.core.pronunciation

import org.json.JSONObject

data class NativeVoicePackManifest(
    val engineFamily: String? = null,
    val modelFamily: String? = null,
    val supportsImportedWords: Boolean = false,
    val estimatedStorageBytes: Long? = null,
    val estimatedRamMb: Int? = null,
    val licenses: List<String> = emptyList(),
) {
    fun isEmpty(): Boolean =
        engineFamily.isNullOrBlank() &&
            modelFamily.isNullOrBlank() &&
            !supportsImportedWords &&
            estimatedStorageBytes == null &&
            estimatedRamMb == null &&
            licenses.isEmpty()

    companion object {
        fun fromCatalogItem(item: JSONObject): NativeVoicePackManifest =
            fromJsonObject(item.optJSONObject("native") ?: item.optJSONObject("runtime") ?: JSONObject())

        fun fromInstalledManifest(item: JSONObject): NativeVoicePackManifest =
            fromJsonObject(item.optJSONObject("native") ?: item)

        private fun fromJsonObject(item: JSONObject): NativeVoicePackManifest =
            NativeVoicePackManifest(
                engineFamily = item.optTrimmedString("engineFamily"),
                modelFamily = item.optTrimmedString("modelFamily"),
                supportsImportedWords = item.optBoolean("supportsImportedWords", false),
                estimatedStorageBytes = item.optNullableLong("estimatedStorageBytes"),
                estimatedRamMb = item.optNullableInt("estimatedRamMb"),
                licenses = item.optLicenseList("licenses"),
            )
    }
}

private fun JSONObject.optTrimmedString(key: String): String? =
    optString(key).trim().takeIf(String::isNotEmpty)

private fun JSONObject.optNullableLong(key: String): Long? =
    if (has(key) && !isNull(key)) {
        optLong(key)
    } else {
        null
    }

private fun JSONObject.optNullableInt(key: String): Int? =
    if (has(key) && !isNull(key)) {
        optInt(key)
    } else {
        null
    }

private fun JSONObject.optLicenseList(key: String): List<String> {
    val licenses = optJSONArray(key) ?: return emptyList()
    return buildList {
        repeat(licenses.length()) { index ->
            val value = licenses.opt(index)
            when (value) {
                is String -> value.trim().takeIf(String::isNotEmpty)?.let(::add)
                is JSONObject -> {
                    listOf("spdx", "id", "name", "file")
                        .firstNotNullOfOrNull(value::optTrimmedString)
                        ?.let(::add)
                }
            }
        }
    }
}
