package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.model.PHONETIC_SOURCE_AI
import com.yueliangmanle.danci.core.model.PHONETIC_STATUS_COMPLETE
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRoundTripTest {
    @Test
    fun settingsJsonIncludesMultiProfileRoutingFields() {
        val settings = AppSettings(
            dailyGoal = 35,
            activeBookId = "cet6",
            aiEnabled = true,
            defaultAiProfileId = "default-profile",
            wordHelpProfileId = "word-help",
            planAdjustmentProfileId = "plan-profile",
            phoneticFillProfileId = "phonetic-profile",
        )

        val json = settings.toJson()

        assertEquals("default-profile", json.getString("default_ai_profile_id"))
        assertEquals("word-help", json.getString("word_help_profile_id"))
        assertEquals("plan-profile", json.getString("plan_adjustment_profile_id"))
        assertEquals("phonetic-profile", json.getString("phonetic_fill_profile_id"))
    }

    @Test
    fun wordJsonIncludesDualPhoneticFields() {
        val word = WordEntity(
            id = 1,
            lemma = "abandon",
            phonetic = "/əˈbændən/",
            phoneticUk = "/əˈbændən/",
            phoneticUs = "/əˈbændən/",
            phoneticSource = PHONETIC_SOURCE_AI,
            phoneticStatus = PHONETIC_STATUS_COMPLETE,
            phoneticUpdatedAt = Instant.parse("2026-03-19T10:00:00Z"),
            meanings = listOf("放弃"),
        )

        val json = word.toJson()

        assertEquals("/əˈbændən/", json.getString("phonetic_uk"))
        assertEquals("/əˈbændən/", json.getString("phonetic_us"))
        assertEquals(PHONETIC_SOURCE_AI, json.getString("phonetic_source"))
        assertEquals(PHONETIC_STATUS_COMPLETE, json.getString("phonetic_status"))
        assertTrue(json.has("phonetic_updated_at"))
    }

    @Test
    fun importerAcceptsLegacyVersionOneBackup() {
        val bytes = zipBackup(
            manifest = JSONObject()
                .put("version", 1)
                .put("created_at", "2026-03-19T10:00:00Z")
                .put("sections", org.json.JSONArray(REQUIRED_BACKUP_SECTIONS)),
            payload = JSONObject()
                .put(
                    "settings",
                    JSONObject()
                        .put("daily_goal", 20)
                        .put("active_book_id", JSONObject.NULL)
                        .put("ai_enabled", false)
                        .put("ai_base_url", "https://api.openai.com/v1")
                        .put("ai_model", "gpt-5-mini")
                        .put("ai_plan_adjustment_enabled", true)
                        .put("ai_session_checkpoint_enabled", true)
                        .put("reminder_enabled", false)
                        .put("reminder_hour", 21)
                        .put("reminder_minute", 0),
                )
                .put("books", org.json.JSONArray())
                .put("book_words", org.json.JSONArray())
                .put(
                    "words",
                    org.json.JSONArray().put(
                        JSONObject()
                            .put("id", 1)
                            .put("lemma", "abandon")
                            .put("phonetic", "/əˈbændən/")
                            .put("part_of_speech", org.json.JSONArray())
                            .put("meanings", org.json.JSONArray().put("放弃"))
                            .put("synonyms", org.json.JSONArray())
                            .put("antonyms", org.json.JSONArray())
                            .put("similar_words", org.json.JSONArray())
                            .put("confusing_words", org.json.JSONArray())
                            .put("word_forms", org.json.JSONArray())
                            .put("tags", org.json.JSONArray()),
                    ),
                )
                .put("learning_records", org.json.JSONArray())
                .put("study_sessions", org.json.JSONArray())
                .put("study_events", org.json.JSONArray())
                .put("ai_memory_summary", JSONObject()),
        )

        val imported = BackupImporter().import(bytes)

        assertEquals(1, imported.manifest.version)
        assertEquals("/əˈbændən/", imported.snapshot.words.single().phonetic)
    }
}

private fun zipBackup(
    manifest: JSONObject,
    payload: JSONObject,
): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        zip.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
        zip.write(manifest.toString().toByteArray())
        zip.closeEntry()

        zip.putNextEntry(ZipEntry(PAYLOAD_FILE_NAME))
        zip.write(payload.toString().toByteArray())
        zip.closeEntry()
    }
    return output.toByteArray()
}
