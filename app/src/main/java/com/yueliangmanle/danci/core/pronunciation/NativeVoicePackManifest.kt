package com.yueliangmanle.danci.core.pronunciation

import org.json.JSONObject

data class NativeVoicePackManifest(
    val id: String? = null,
    val name: String? = null,
    val accent: String? = null,
    val locale: String? = null,
    val engineType: String? = null,
    val engineFamily: String? = null,
    val modelFamily: String? = null,
    val modelVersion: String? = null,
    val packageFormatVersion: Int? = null,
    val entryFiles: List<String> = emptyList(),
    val payloadChecksums: Map<String, String> = emptyMap(),
    val estimatedStorageBytes: Long? = null,
    val estimatedRamMb: Int? = null,
    val speakerProfile: String? = null,
    val licenses: List<NativeVoicePackLicense> = emptyList(),
    val supportsImportedWords: Boolean = false,
) {
    fun isEmpty(): Boolean =
        id.isNullOrBlank() &&
            name.isNullOrBlank() &&
            accent.isNullOrBlank() &&
            locale.isNullOrBlank() &&
            engineType.isNullOrBlank() &&
            engineFamily.isNullOrBlank() &&
            modelFamily.isNullOrBlank() &&
            modelVersion.isNullOrBlank() &&
            packageFormatVersion == null &&
            entryFiles.isEmpty() &&
            payloadChecksums.isEmpty() &&
            estimatedStorageBytes == null &&
            estimatedRamMb == null &&
            speakerProfile == null &&
            licenses.isEmpty() &&
            !supportsImportedWords

    fun licenseLabels(): List<String> =
        licenses.mapNotNull(NativeVoicePackLicense::label)

    fun referencedLicenseFiles(): List<String> =
        licenses.mapNotNull(NativeVoicePackLicense::file)

    companion object {
        fun fromCatalogItem(item: JSONObject): NativeVoicePackManifest =
            if (item.optJSONObject("native") != null || item.optJSONObject("runtime") != null) {
                fromJsonObject(item)
            } else {
                NativeVoicePackManifest()
            }

        fun fromInstalledManifest(item: JSONObject): NativeVoicePackManifest =
            fromJsonObject(item)

        private fun fromJsonObject(root: JSONObject): NativeVoicePackManifest {
            val nativeBlock = root.optJSONObject("native") ?: root.optJSONObject("runtime")
            return NativeVoicePackManifest(
                id = root.optTrimmedString("id") ?: nativeBlock?.optTrimmedString("id"),
                name = root.optTrimmedString("name") ?: nativeBlock?.optTrimmedString("name"),
                accent = root.optTrimmedString("accent") ?: nativeBlock?.optTrimmedString("accent"),
                locale = root.optTrimmedString("locale") ?: nativeBlock?.optTrimmedString("locale"),
                engineType = nativeBlock?.optTrimmedString("engineType") ?: root.optTrimmedString("engineType"),
                engineFamily = nativeBlock?.optTrimmedString("engineFamily")
                    ?: nativeBlock?.optTrimmedString("engineType")
                    ?: root.optTrimmedString("engineFamily")
                    ?: root.optTrimmedString("engineType"),
                modelFamily = nativeBlock?.optTrimmedString("modelFamily") ?: root.optTrimmedString("modelFamily"),
                modelVersion = nativeBlock?.optTrimmedString("modelVersion")
                    ?: root.optTrimmedString("modelVersion")
                    ?: root.optTrimmedString("version"),
                packageFormatVersion = nativeBlock?.optNullableInt("packageFormatVersion")
                    ?: root.optNullableInt("packageFormatVersion"),
                entryFiles = nativeBlock.optStringList("entryFiles").ifEmpty {
                    root.optStringList("entryFiles")
                },
                payloadChecksums = nativeBlock.optStringMap("payloadChecksums").ifEmpty {
                    root.optStringMap("payloadChecksums")
                },
                estimatedStorageBytes = nativeBlock?.optNullableLong("estimatedStorageBytes")
                    ?: root.optNullableLong("estimatedStorageBytes"),
                estimatedRamMb = nativeBlock?.optNullableInt("estimatedRamMb")
                    ?: root.optNullableInt("estimatedRamMb"),
                speakerProfile = nativeBlock.optSpeakerProfile("speakerProfile")
                    ?: root.optSpeakerProfile("speakerProfile"),
                licenses = nativeBlock.optLicenseList("licenses").ifEmpty {
                    root.optLicenseList("licenses")
                },
                supportsImportedWords = nativeBlock?.optBoolean(
                    "supportsImportedWords",
                    root.optBoolean("supportsImportedWords", false),
                ) ?: root.optBoolean("supportsImportedWords", false),
            )
        }
    }
}

data class NativeVoicePackLicense(
    val spdx: String? = null,
    val id: String? = null,
    val name: String? = null,
    val file: String? = null,
) {
    fun label(): String? =
        listOf(spdx, id, name)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
}

private fun JSONObject?.optTrimmedString(key: String): String? =
    this?.optString(key).orEmpty().trim().takeIf(String::isNotEmpty)

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

private fun JSONObject?.optStringList(key: String): List<String> {
    val items = this?.optJSONArray(key) ?: return emptyList()
    return buildList {
        repeat(items.length()) { index ->
            items.optString(index)
                .trim()
                .takeIf(String::isNotEmpty)
                ?.let(::add)
        }
    }
}

private fun JSONObject?.optStringMap(key: String): Map<String, String> {
    val values = this?.optJSONObject(key) ?: return emptyMap()
    return buildMap {
        values.keys().forEach { entryKey ->
            values.optString(entryKey)
                .trim()
                .takeIf(String::isNotEmpty)
                ?.let { put(entryKey.trim(), it) }
        }
    }.filterKeys(String::isNotBlank)
}

private fun JSONObject?.optSpeakerProfile(key: String): String? {
    val container = this ?: return null
    container.optString(key)
        .trim()
        .takeIf(String::isNotEmpty)
        ?.let { return it }
    val profile = container.optJSONObject(key) ?: return null
    return listOf("displayName", "name", "id", "accent")
        .firstNotNullOfOrNull(profile::optTrimmedString)
}

private fun JSONObject?.optLicenseList(key: String): List<NativeVoicePackLicense> {
    val licenses = this?.optJSONArray(key) ?: return emptyList()
    return buildList {
        repeat(licenses.length()) { index ->
            when (val value = licenses.opt(index)) {
                is String -> value.trim()
                    .takeIf(String::isNotEmpty)
                    ?.let { add(NativeVoicePackLicense(name = it)) }
                is JSONObject -> add(
                    NativeVoicePackLicense(
                        spdx = value.optTrimmedString("spdx"),
                        id = value.optTrimmedString("id"),
                        name = value.optTrimmedString("name"),
                        file = value.optTrimmedString("file"),
                    ),
                )
            }
        }
    }
}
