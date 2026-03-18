package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupImporterTest {
    @Test
    fun importRestoresSnapshotFromZipArchive() = runTest {
        val archive = BackupExporter(
            snapshotProvider = {
                BackupSnapshot(
                    settings = AppSettings(
                        dailyGoal = 35,
                        activeBookId = "cet4",
                        aiEnabled = true,
                        aiBaseUrl = "https://api.openai.com/v1",
                        aiModel = "gpt-5-mini",
                        reminderEnabled = true,
                        reminderHour = 21,
                        reminderMinute = 30,
                    ),
                    books = listOf(
                        BookEntity(
                            id = "cet4",
                            title = "四级核心词",
                            wordCount = 1,
                            createdAt = Instant.parse("2026-03-18T12:00:00Z"),
                            updatedAt = Instant.parse("2026-03-18T12:00:00Z"),
                        ),
                    ),
                    words = listOf(
                        WordEntity(
                            id = 1L,
                            lemma = "abandon",
                            meanings = listOf("放弃"),
                            synonyms = listOf("give up"),
                            wordForms = listOf("abandoned", "abandoning"),
                        ),
                    ),
                    aiMemorySummary = AiMemorySummary(
                        learnerProfile = LearnerProfile(
                            vocabularyLevel = "提升中",
                            weakSpots = listOf("abandon"),
                            updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                        ),
                        dailySummaries = listOf(
                            DailySummary(
                                date = "2026-03-18",
                                studiedCount = 24,
                                reviewCount = 10,
                                correctRate = 0.75f,
                                updatedAt = Instant.parse("2026-03-18T08:00:00Z"),
                            ),
                        ),
                    ),
                )
            },
            nowProvider = { Instant.parse("2026-03-18T12:30:00Z") },
        ).export()

        val imported = BackupImporter().import(archive.zippedBytes)

        assertEquals(35, imported.snapshot.settings.dailyGoal)
        assertEquals("cet4", imported.snapshot.settings.activeBookId)
        assertEquals("abandon", imported.snapshot.words.single().lemma)
        assertEquals("提升中", imported.snapshot.aiMemorySummary.learnerProfile?.vocabularyLevel)
        assertEquals(1, imported.snapshot.aiMemorySummary.dailySummaries.size)
    }

    @Test
    fun importRejectsArchiveMissingRequiredSections() {
        val invalidManifest = BackupManifest(
            version = BACKUP_VERSION,
            createdAt = "2026-03-18T12:30:00Z",
            sections = listOf("books", "words"),
        )
        val payload = BackupSnapshot(
            settings = AppSettings(),
        ).toJson().toString()

        val error = assertThrows(IllegalArgumentException::class.java) {
            BackupImporter().import(
                zipEntries(
                    manifest = invalidManifest.toJson().toString(),
                    payload = payload,
                ),
            )
        }

        assertEquals("Missing required backup section: settings", error.message)
    }

    private fun zipEntries(
        manifest: String,
        payload: String,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(PAYLOAD_FILE_NAME))
            zip.write(payload.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
