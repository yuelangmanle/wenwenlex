package com.yueliangmanle.danci.core.diagnostics

import com.yueliangmanle.danci.core.data.AppSettings
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataIntegrityVerifierTest {

    @Test
    fun verify_reports_missing_goal_settings_and_empty_learning_records() {
        val report = DataIntegrityVerifier().verify(
            DataIntegritySnapshot(
                checkedAt = Instant.parse("2026-03-22T12:00:00Z"),
                settings = AppSettings(
                    dailyGoal = 0,
                    weeklyGoal = 0,
                ),
                learningRecordCount = 0,
                studyEventCount = 0,
                bookCount = 0,
                aiProfileCount = 0,
                installedVoicePackCount = 0,
                hasBackup = false,
            ),
        )

        assertEquals(5, report.issues.size)
        assertTrue(report.issues.any { it.code == "missing_goal_settings" })
        assertTrue(report.issues.any { it.code == "empty_learning_records" })
        assertTrue(report.issues.any { it.code == "empty_study_events" })
        assertTrue(report.issues.any { it.code == "missing_books" })
        assertTrue(report.issues.any { it.code == "missing_backup" })
        assertEquals("needs_attention", report.status)
    }

    @Test
    fun verify_reports_ai_profile_mismatch_when_ai_enabled_without_profile() {
        val report = DataIntegrityVerifier().verify(
            DataIntegritySnapshot(
                checkedAt = Instant.parse("2026-03-22T12:00:00Z"),
                settings = AppSettings(
                    aiEnabled = true,
                    defaultAiProfileId = "default-openai",
                ),
                learningRecordCount = 18,
                studyEventCount = 44,
                bookCount = 2,
                aiProfileCount = 0,
                installedVoicePackCount = 1,
                hasBackup = true,
            ),
        )

        assertTrue(report.issues.any { it.code == "missing_default_ai_profile" })
        assertEquals("needs_attention", report.status)
    }

    @Test
    fun verify_is_healthy_when_core_learning_surfaces_exist() {
        val report = DataIntegrityVerifier().verify(
            DataIntegritySnapshot(
                checkedAt = Instant.parse("2026-03-22T12:00:00Z"),
                settings = AppSettings(
                    dailyGoal = 20,
                    weeklyGoal = 140,
                    aiEnabled = true,
                    defaultAiProfileId = "default-openai",
                    activeBookId = "cet4",
                    activeVoicePackId = "en-us-offline-word-v1",
                ),
                learningRecordCount = 42,
                studyEventCount = 180,
                bookCount = 5,
                aiProfileCount = 2,
                installedVoicePackCount = 1,
                hasBackup = true,
            ),
        )

        assertTrue(report.issues.isEmpty())
        assertEquals("healthy", report.status)
    }
}
