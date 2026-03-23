package com.yueliangmanle.danci.core.releasenotes

import android.content.Context
import com.yueliangmanle.danci.core.model.ReleaseNote
import com.yueliangmanle.danci.core.model.ReleaseNoteSection
import org.json.JSONArray
import org.json.JSONObject

private const val RELEASE_NOTES_ASSET_PATH = "release-notes/release-notes.json"

class ReleaseNotesCatalog(
    private val assetLoader: () -> String,
) {
    fun load(currentVersion: String): List<ReleaseNote> =
        parseJson(
            jsonText = assetLoader(),
            currentVersion = currentVersion,
        )

    internal fun parseJson(
        jsonText: String,
        currentVersion: String,
    ): List<ReleaseNote> {
        val root = JSONObject(jsonText)
        val releases = root.optJSONArray("releases") ?: JSONArray()
        return buildList {
            for (index in 0 until releases.length()) {
                val item = releases.optJSONObject(index) ?: continue
                add(
                    ReleaseNote(
                        version = item.optString("version"),
                        date = item.optString("date"),
                        sections = parseSections(item.optJSONArray("sections")),
                        isCurrent = item.optString("version") == currentVersion,
                    ),
                )
            }
        }
    }

    private fun parseSections(
        sections: JSONArray?,
    ): List<ReleaseNoteSection> =
        buildList {
            for (index in 0 until (sections?.length() ?: 0)) {
                val section = sections?.optJSONObject(index) ?: continue
                val items = section.optJSONArray("items") ?: JSONArray()
                add(
                    ReleaseNoteSection(
                        title = section.optString("title"),
                        items = buildList {
                            for (itemIndex in 0 until items.length()) {
                                items.optString(itemIndex)
                                    .takeIf(String::isNotBlank)
                                    ?.let(::add)
                            }
                        },
                    ),
                )
            }
        }
}

fun buildReleaseNotesCatalog(context: Context): ReleaseNotesCatalog =
    ReleaseNotesCatalog(
        assetLoader = {
            context.applicationContext.assets.open(RELEASE_NOTES_ASSET_PATH)
                .bufferedReader()
                .use { it.readText() }
        },
    )
