package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.StudyEventEntity
import com.yueliangmanle.danci.core.database.entity.StudySessionEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.ConfusionEdge
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.WeeklySummary
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

class BackupExporter(
    private val snapshotProvider: suspend () -> BackupSnapshot,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    suspend fun export(): BackupArchive {
        val snapshot = snapshotProvider()
        val payloadJson = snapshot.toJson().toString()
        val manifest = BackupManifest(
            version = BACKUP_VERSION,
            createdAt = nowProvider().toString(),
            sections = buildBackupSections(),
        )
        return BackupArchive(
            manifest = manifest,
            serializedJson = payloadJson,
            zippedBytes = zipBackup(manifest, payloadJson),
        )
    }

    private fun buildBackupSections(): List<String> = REQUIRED_BACKUP_SECTIONS + listOf(
        "books",
        "words",
        "study_data",
    )

    private fun zipBackup(
        manifest: BackupManifest,
        payloadJson: String,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
            zip.write(manifest.toJson().toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(PAYLOAD_FILE_NAME))
            zip.write(payloadJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}

internal fun BackupManifest.toJson(): JSONObject =
    JSONObject()
        .put("version", version)
        .put("created_at", createdAt)
        .put("sections", JSONArray(sections))

internal fun BackupSnapshot.toJson(): JSONObject =
    JSONObject()
        .put("settings", settings.toJson())
        .put("books", JSONArray(books.map(BookEntity::toJson)))
        .put("book_words", JSONArray(bookWords.map(BookWordEntity::toJson)))
        .put("words", JSONArray(words.map(WordEntity::toJson)))
        .put("learning_records", JSONArray(learningRecords.map(LearningRecordEntity::toJson)))
        .put("study_sessions", JSONArray(studySessions.map(StudySessionEntity::toJson)))
        .put("study_events", JSONArray(studyEvents.map(StudyEventEntity::toJson)))
        .put("ai_memory_summary", aiMemorySummary.toJson())

internal fun AppSettings.toJson(): JSONObject =
    JSONObject()
        .put("daily_goal", dailyGoal)
        .put("active_book_id", activeBookId)
        .put("ai_enabled", aiEnabled)
        .put("ai_base_url", aiBaseUrl)
        .put("ai_model", aiModel)
        .put("ai_plan_adjustment_enabled", aiPlanAdjustmentEnabled)
        .put("ai_session_checkpoint_enabled", aiSessionCheckpointEnabled)
        .put("reminder_enabled", reminderEnabled)
        .put("reminder_hour", reminderHour)
        .put("reminder_minute", reminderMinute)

internal fun BookEntity.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("title", title)
        .put("description", description)
        .put("language", language)
        .put("category", category)
        .put("source_type", sourceType)
        .put("word_count", wordCount)
        .put("created_at", createdAt.toBackupString())
        .put("updated_at", updatedAt.toBackupString())

internal fun BookWordEntity.toJson(): JSONObject =
    JSONObject()
        .put("book_id", bookId)
        .put("word_id", wordId)
        .put("chapter", chapter)
        .put("sort_order", sortOrder)
        .put("tags", JSONArray(tags))
        .put("note", note)

internal fun WordEntity.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("lemma", lemma)
        .put("phonetic", phonetic)
        .put("part_of_speech", JSONArray(partOfSpeech))
        .put("meanings", JSONArray(meanings))
        .put("example_sentence", exampleSentence)
        .put("example_translation", exampleTranslation)
        .put("synonyms", JSONArray(synonyms))
        .put("antonyms", JSONArray(antonyms))
        .put("similar_words", JSONArray(similarWords))
        .put("confusing_words", JSONArray(confusingWords))
        .put("word_forms", JSONArray(wordForms))
        .put("root", root)
        .put("tags", JSONArray(tags))
        .put("frequency_rank", frequencyRank)
        .put("pronunciation_url", pronunciationUrl)

internal fun LearningRecordEntity.toJson(): JSONObject =
    JSONObject()
        .put("word_id", wordId)
        .put("mastery", mastery.toDouble())
        .put("familiarity_state", familiarityState)
        .put("next_review_at", nextReviewAt.toBackupString())
        .put("review_count", reviewCount)
        .put("lapse_count", lapseCount)
        .put("consecutive_correct_count", consecutiveCorrectCount)
        .put("last_reviewed_at", lastReviewedAt.toBackupString())
        .put("last_outcome", lastOutcome)
        .put("confusion_weight", confusionWeight.toDouble())
        .put("similar_spelling_weight", similarSpellingWeight.toDouble())

internal fun StudySessionEntity.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("mode", mode)
        .put("target_book_id", targetBookId)
        .put("started_at", startedAt.toBackupString())
        .put("finished_at", finishedAt.toBackupString())
        .put("planned_count", plannedCount)
        .put("completed_count", completedCount)
        .put("correct_count", correctCount)
        .put("wrong_count", wrongCount)
        .put("strategy_snapshot", strategySnapshot)

internal fun StudyEventEntity.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("session_id", sessionId)
        .put("word_id", wordId)
        .put("event_type", eventType)
        .put("feedback", feedback)
        .put("is_correct", isCorrect)
        .put("happened_at", happenedAt.toBackupString())
        .put("elapsed_millis", elapsedMillis)
        .put("metadata", metadata)

internal fun AiMemorySummary.toJson(): JSONObject =
    JSONObject()
        .put("learner_profile", learnerProfile?.toJson() ?: JSONObject())
        .put("daily_summaries", JSONArray(dailySummaries.map(DailySummary::toJson)))
        .put("weekly_summaries", JSONArray(weeklySummaries.map(WeeklySummary::toJson)))
        .put("plan_history", JSONArray(planHistory.map(PlanHistoryEntry::toJson)))
        .put("confusion_edges", JSONArray(confusionEdges.map(ConfusionEdge::toJson)))

internal fun LearnerProfile.toJson(): JSONObject =
    JSONObject()
        .put("profile_id", profileId)
        .put("vocabulary_level", vocabularyLevel)
        .put("weak_spots", JSONArray(weakSpots))
        .put("preferred_question_types", JSONArray(preferredQuestionTypes))
        .put("common_mistake_patterns", JSONArray(commonMistakePatterns))
        .put("updated_at", updatedAt.toBackupString())

internal fun DailySummary.toJson(): JSONObject =
    JSONObject()
        .put("date", date)
        .put("studied_count", studiedCount)
        .put("review_count", reviewCount)
        .put("correct_rate", correctRate.toDouble())
        .put("fatigue_note", fatigueNote)
        .put("primary_mistake_reasons", JSONArray(primaryMistakeReasons))
        .put("updated_at", updatedAt.toBackupString())

internal fun WeeklySummary.toJson(): JSONObject =
    JSONObject()
        .put("week_start_date", weekStartDate)
        .put("studied_count", studiedCount)
        .put("correct_rate", correctRate.toDouble())
        .put("trend_summary", trendSummary)
        .put("persistent_weak_spots", JSONArray(persistentWeakSpots))
        .put("updated_at", updatedAt.toBackupString())

internal fun PlanHistoryEntry.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("generated_at", generatedAt.toBackupString())
        .put("summary", summary)
        .put("recommended_focus", JSONArray(recommendedFocus))
        .put("suggested_pace", suggestedPace)
        .put("execution_effect", executionEffect)

internal fun ConfusionEdge.toJson(): JSONObject =
    JSONObject()
        .put("source_word_id", sourceWordId)
        .put("target_word_id", targetWordId)
        .put("relation_type", relationType)
        .put("weight", weight.toDouble())
        .put("mistake_count", mistakeCount)
        .put("updated_at", updatedAt.toBackupString())
