package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.AiProviderProfileEntity
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.database.entity.ImportBatchEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.PhoneticEnrichmentJobEntity
import com.yueliangmanle.danci.core.database.entity.StudyEventEntity
import com.yueliangmanle.danci.core.database.entity.StudySessionEntity
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.database.entity.WordAudioAssetEntity
import com.yueliangmanle.danci.core.model.PHONETIC_SOURCE_EMPTY
import com.yueliangmanle.danci.core.model.PHONETIC_SOURCE_LEGACY
import com.yueliangmanle.danci.core.model.PHONETIC_STATUS_EMPTY
import com.yueliangmanle.danci.core.model.PHONETIC_STATUS_PARTIAL
import com.yueliangmanle.danci.core.model.AiMemorySummary
import com.yueliangmanle.danci.core.model.ConfusionEdge
import com.yueliangmanle.danci.core.model.DailySummary
import com.yueliangmanle.danci.core.model.LearnerProfile
import com.yueliangmanle.danci.core.model.PlanHistoryEntry
import com.yueliangmanle.danci.core.model.WeeklySummary
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.json.JSONArray
import org.json.JSONObject

class BackupImporter {
    fun import(bytes: ByteArray): ImportedBackup {
        val entries = unzip(bytes)
        val manifestJson = requireNotNull(entries[MANIFEST_FILE_NAME]) { "Missing $MANIFEST_FILE_NAME" }
        val payloadJson = requireNotNull(entries[PAYLOAD_FILE_NAME]) { "Missing $PAYLOAD_FILE_NAME" }
        val manifest = manifestJson.toBackupManifest()
        require(manifest.version in SUPPORTED_BACKUP_VERSIONS) { "Unsupported backup version: ${manifest.version}" }
        REQUIRED_BACKUP_SECTIONS.forEach { section ->
            require(section in manifest.sections) { "Missing required backup section: $section" }
        }
        return ImportedBackup(
            manifest = manifest,
            snapshot = payloadJson.toBackupSnapshot(manifest.version),
        )
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        return entries
    }
}

internal fun String.toBackupManifest(): BackupManifest {
    val json = JSONObject(this)
    return BackupManifest(
        version = json.getInt("version"),
        createdAt = json.getString("created_at"),
        sections = json.getJSONArray("sections").toStringList(),
    )
}

internal fun String.toBackupSnapshot(version: Int = BACKUP_VERSION): BackupSnapshot {
    val json = JSONObject(this)
    return BackupSnapshot(
        settings = json.getJSONObject("settings").toAppSettings(),
        aiProfiles = json.optJSONArray("ai_profiles").mapObjects(JSONObject::toAiProviderProfileEntity),
        books = json.getJSONArray("books").mapObjects(JSONObject::toBookEntity),
        bookWords = json.getJSONArray("book_words").mapObjects(JSONObject::toBookWordEntity),
        words = json.getJSONArray("words").mapObjects { toWordEntity(version) },
        wordAudioAssets = json.optJSONArray("word_audio_assets").mapObjects(JSONObject::toWordAudioAssetEntity),
        voicePacks = json.optJSONArray("voice_packs").mapObjects(JSONObject::toVoicePackEntity),
        importBatches = json.optJSONArray("import_batches").mapObjects(JSONObject::toImportBatchEntity),
        phoneticEnrichmentJobs = json.optJSONArray("phonetic_enrichment_jobs").mapObjects(JSONObject::toPhoneticEnrichmentJobEntity),
        learningRecords = json.getJSONArray("learning_records").mapObjects(JSONObject::toLearningRecordEntity),
        studySessions = json.getJSONArray("study_sessions").mapObjects(JSONObject::toStudySessionEntity),
        studyEvents = json.getJSONArray("study_events").mapObjects(JSONObject::toStudyEventEntity),
        aiMemorySummary = json.getJSONObject("ai_memory_summary").toAiMemorySummary(),
    )
}

internal fun JSONObject.toAppSettings(): AppSettings =
    AppSettings(
        dailyGoal = getInt("daily_goal"),
        activeBookId = optNullableString("active_book_id"),
        aiEnabled = getBoolean("ai_enabled"),
        aiBaseUrl = optNullableString("ai_base_url") ?: com.yueliangmanle.danci.core.data.DEFAULT_AI_BASE_URL,
        aiModel = optNullableString("ai_model") ?: com.yueliangmanle.danci.core.data.DEFAULT_AI_MODEL,
        defaultAiProfileId = optNullableString("default_ai_profile_id"),
        wordHelpProfileId = optNullableString("word_help_profile_id"),
        planAdjustmentProfileId = optNullableString("plan_adjustment_profile_id"),
        phoneticFillProfileId = optNullableString("phonetic_fill_profile_id"),
        aiPlanAdjustmentEnabled = getBoolean("ai_plan_adjustment_enabled"),
        aiSessionCheckpointEnabled = getBoolean("ai_session_checkpoint_enabled"),
        preferredPronunciationAccent = optNullableString("preferred_pronunciation_accent")
            ?: com.yueliangmanle.danci.core.model.DEFAULT_PRONUNCIATION_ACCENT,
        pronunciationMode = optNullableString("pronunciation_mode")
            ?: com.yueliangmanle.danci.core.model.DEFAULT_PRONUNCIATION_MODE,
        allowCellularVoicePackDownload = optBoolean("allow_cellular_voice_pack_download", false),
        autoCacheWordAudio = optBoolean("auto_cache_word_audio", true),
        audioCacheLimitMb = optInt(
            "audio_cache_limit_mb",
            com.yueliangmanle.danci.core.model.DEFAULT_AUDIO_CACHE_LIMIT_MB,
        ),
        activeVoicePackId = optNullableString("active_voice_pack_id"),
        fallbackToSystemTts = optBoolean("fallback_to_system_tts", true),
        preferOfflineForLongText = optBoolean("prefer_offline_for_long_text", true),
        reminderEnabled = optBoolean("reminder_enabled", false),
        reminderHour = optInt("reminder_hour", 21),
        reminderMinute = optInt("reminder_minute", 0),
    )

internal fun JSONObject.toAiProviderProfileEntity(): AiProviderProfileEntity =
    AiProviderProfileEntity(
        id = getString("id"),
        name = getString("name"),
        providerType = optNullableString("provider_type") ?: "custom",
        baseUrl = getString("base_url"),
        model = getString("model"),
        enabled = optBoolean("enabled", true),
        createdAt = optInstant("created_at") ?: java.time.Instant.EPOCH,
        updatedAt = optInstant("updated_at") ?: java.time.Instant.EPOCH,
        lastValidatedAt = optInstant("last_validated_at"),
    )

internal fun JSONObject.toBookEntity(): BookEntity =
    BookEntity(
        id = getString("id"),
        title = getString("title"),
        description = optNullableString("description"),
        language = optNullableString("language") ?: "en",
        category = optNullableString("category") ?: "general",
        sourceType = optNullableString("source_type") ?: "builtin",
        wordCount = optInt("word_count", 0),
        createdAt = optInstant("created_at") ?: java.time.Instant.EPOCH,
        updatedAt = optInstant("updated_at") ?: java.time.Instant.EPOCH,
    )

internal fun JSONObject.toBookWordEntity(): BookWordEntity =
    BookWordEntity(
        bookId = getString("book_id"),
        wordId = getLong("word_id"),
        chapter = optNullableString("chapter"),
        sortOrder = optInt("sort_order", 0),
        tags = optJSONArray("tags").toStringList(),
        note = optNullableString("note"),
    )

internal fun JSONObject.toWordEntity(version: Int = BACKUP_VERSION): WordEntity {
    val phonetic = optNullableString("phonetic")
    val phoneticUk = optNullableString("phonetic_uk")
        ?: phonetic.takeIf { version == 1 && !it.isNullOrBlank() }
    val phoneticUs = optNullableString("phonetic_us")
    return WordEntity(
        id = getLong("id"),
        lemma = getString("lemma"),
        phonetic = phonetic,
        phoneticUk = phoneticUk,
        phoneticUs = phoneticUs,
        phoneticSource = optNullableString("phonetic_source") ?: when {
            !phonetic.isNullOrBlank() -> PHONETIC_SOURCE_LEGACY
            else -> PHONETIC_SOURCE_EMPTY
        },
        phoneticStatus = optNullableString("phonetic_status") ?: when {
            !phonetic.isNullOrBlank() -> PHONETIC_STATUS_PARTIAL
            else -> PHONETIC_STATUS_EMPTY
        },
        phoneticUpdatedAt = optInstant("phonetic_updated_at"),
        partOfSpeech = optJSONArray("part_of_speech").toStringList(),
        meanings = optJSONArray("meanings").toStringList(),
        exampleSentence = optNullableString("example_sentence"),
        exampleTranslation = optNullableString("example_translation"),
        synonyms = optJSONArray("synonyms").toStringList(),
        antonyms = optJSONArray("antonyms").toStringList(),
        similarWords = optJSONArray("similar_words").toStringList(),
        confusingWords = optJSONArray("confusing_words").toStringList(),
        wordForms = optJSONArray("word_forms").toStringList(),
        root = optNullableString("root"),
        tags = optJSONArray("tags").toStringList(),
        frequencyRank = optIntOrNull("frequency_rank"),
        pronunciationUrl = optNullableString("pronunciation_url"),
    )
}

internal fun JSONObject.toWordAudioAssetEntity(): WordAudioAssetEntity =
    WordAudioAssetEntity(
        id = optLongOrNull("id") ?: 0L,
        wordId = getLong("word_id"),
        accent = optNullableString("accent") ?: "auto",
        sourceType = optNullableString("source_type") ?: "dictionary_cache",
        remoteUrl = optNullableString("remote_url"),
        localPath = optNullableString("local_path"),
        mimeType = optNullableString("mime_type"),
        checksum = optNullableString("checksum"),
        status = optNullableString("status") ?: "empty",
        fetchedAt = optInstant("fetched_at"),
        lastPlayedAt = optInstant("last_played_at"),
        lastError = optNullableString("last_error"),
        failureCount = optInt("failure_count", 0),
    )

internal fun JSONObject.toVoicePackEntity(): VoicePackEntity =
    VoicePackEntity(
        id = getString("id"),
        name = getString("name"),
        locale = optNullableString("locale") ?: "en-US",
        accent = optNullableString("accent") ?: "auto",
        engineType = optNullableString("engine_type") ?: "sherpa_onnx",
        version = optNullableString("version") ?: "1",
        downloadUrl = optNullableString("download_url"),
        manifestUrl = optNullableString("manifest_url"),
        installDir = optNullableString("install_dir"),
        archiveChecksum = optNullableString("archive_checksum"),
        installedSizeBytes = optLongOrNull("installed_size_bytes") ?: 0L,
        status = optNullableString("status") ?: "not_installed",
        isActive = optBoolean("is_active", false),
        createdAt = optInstant("created_at") ?: java.time.Instant.EPOCH,
        updatedAt = optInstant("updated_at") ?: java.time.Instant.EPOCH,
    )

internal fun JSONObject.toImportBatchEntity(): ImportBatchEntity =
    ImportBatchEntity(
        id = optLongOrNull("id") ?: 0L,
        bookId = getString("book_id"),
        fileName = getString("file_name"),
        sheetName = optNullableString("sheet_name"),
        parserMode = optNullableString("parser_mode") ?: "strict",
        totalRows = optInt("total_rows", 0),
        importedRows = optInt("imported_rows", 0),
        skippedRows = optInt("skipped_rows", 0),
        aiNormalizedCount = optInt("ai_normalized_count", 0),
        aiCompletedCount = optInt("ai_completed_count", 0),
        createdAt = optInstant("created_at") ?: java.time.Instant.EPOCH,
    )

internal fun JSONObject.toPhoneticEnrichmentJobEntity(): PhoneticEnrichmentJobEntity =
    PhoneticEnrichmentJobEntity(
        id = optLongOrNull("id") ?: 0L,
        scopeType = getString("scope_type"),
        scopeRef = getString("scope_ref"),
        profileId = optNullableString("profile_id"),
        fillMode = getString("fill_mode"),
        status = getString("status"),
        totalCount = optInt("total_count", 0),
        completedCount = optInt("completed_count", 0),
        failedCount = optInt("failed_count", 0),
        createdAt = optInstant("created_at") ?: java.time.Instant.EPOCH,
        updatedAt = optInstant("updated_at") ?: java.time.Instant.EPOCH,
    )

internal fun JSONObject.toLearningRecordEntity(): LearningRecordEntity =
    LearningRecordEntity(
        wordId = getLong("word_id"),
        mastery = getDouble("mastery").toFloat(),
        familiarityState = getString("familiarity_state"),
        reviewStage = optInt("review_stage", 0),
        learningStage = optNullableString("learning_stage") ?: "UNSEEN",
        introducedAt = optInstant("introduced_at"),
        nextReviewAt = optInstant("next_review_at"),
        reviewCount = optInt("review_count", 0),
        lapseCount = optInt("lapse_count", 0),
        consecutiveCorrectCount = optInt("consecutive_correct_count", 0),
        lastReviewedAt = optInstant("last_reviewed_at"),
        lastOutcome = optNullableString("last_outcome"),
        lastMistakeAt = optInstant("last_mistake_at"),
        lastFuzzyAt = optInstant("last_fuzzy_at"),
        lastStudyMode = optNullableString("last_study_mode"),
        currentGroupPassState = optNullableString("current_group_pass_state"),
        confusionWeight = optDouble("confusion_weight", 0.0).toFloat(),
        similarSpellingWeight = optDouble("similar_spelling_weight", 0.0).toFloat(),
    )

internal fun JSONObject.toStudySessionEntity(): StudySessionEntity =
    StudySessionEntity(
        id = getLong("id"),
        mode = getString("mode"),
        targetBookId = optNullableString("target_book_id"),
        scopeType = optNullableString("scope_type") ?: "book",
        scopeRef = optNullableString("scope_ref"),
        groupSize = optInt("group_size", 5),
        currentGroupIndex = optInt("current_group_index", 0),
        startedAt = requireNotNull(optInstant("started_at")),
        finishedAt = optInstant("finished_at"),
        plannedCount = optInt("planned_count", 0),
        completedCount = optInt("completed_count", 0),
        correctCount = optInt("correct_count", 0),
        wrongCount = optInt("wrong_count", 0),
        strategySnapshot = optNullableString("strategy_snapshot"),
    )

internal fun JSONObject.toStudyEventEntity(): StudyEventEntity =
    StudyEventEntity(
        id = getLong("id"),
        sessionId = optLongOrNull("session_id"),
        wordId = getLong("word_id"),
        eventType = getString("event_type"),
        feedback = optNullableString("feedback"),
        isCorrect = optBooleanOrNull("is_correct"),
        happenedAt = requireNotNull(optInstant("happened_at")),
        elapsedMillis = optLongOrNull("elapsed_millis"),
        metadata = optNullableString("metadata"),
    )

internal fun JSONObject.toAiMemorySummary(): AiMemorySummary =
    AiMemorySummary(
        learnerProfile = optJSONObject("learner_profile")?.takeIf { it.length() > 0 }?.toLearnerProfile(),
        dailySummaries = optJSONArray("daily_summaries").mapObjects(JSONObject::toDailySummary),
        weeklySummaries = optJSONArray("weekly_summaries").mapObjects(JSONObject::toWeeklySummary),
        planHistory = optJSONArray("plan_history").mapObjects(JSONObject::toPlanHistoryEntry),
        confusionEdges = optJSONArray("confusion_edges").mapObjects(JSONObject::toConfusionEdge),
    )

internal fun JSONObject.toLearnerProfile(): LearnerProfile =
    LearnerProfile(
        profileId = optNullableString("profile_id") ?: LearnerProfile.DEFAULT_PROFILE_ID,
        vocabularyLevel = optNullableString("vocabulary_level"),
        weakSpots = optJSONArray("weak_spots").toStringList(),
        preferredQuestionTypes = optJSONArray("preferred_question_types").toStringList(),
        commonMistakePatterns = optJSONArray("common_mistake_patterns").toStringList(),
        updatedAt = optInstant("updated_at") ?: java.time.Instant.EPOCH,
    )

internal fun JSONObject.toDailySummary(): DailySummary =
    DailySummary(
        date = getString("date"),
        studiedCount = getInt("studied_count"),
        reviewCount = getInt("review_count"),
        correctRate = getDouble("correct_rate").toFloat(),
        fatigueNote = optNullableString("fatigue_note"),
        primaryMistakeReasons = optJSONArray("primary_mistake_reasons").toStringList(),
        updatedAt = optInstant("updated_at") ?: java.time.Instant.EPOCH,
    )

internal fun JSONObject.toWeeklySummary(): WeeklySummary =
    WeeklySummary(
        weekStartDate = getString("week_start_date"),
        studiedCount = getInt("studied_count"),
        correctRate = getDouble("correct_rate").toFloat(),
        trendSummary = optNullableString("trend_summary"),
        persistentWeakSpots = optJSONArray("persistent_weak_spots").toStringList(),
        updatedAt = optInstant("updated_at") ?: java.time.Instant.EPOCH,
    )

internal fun JSONObject.toPlanHistoryEntry(): PlanHistoryEntry =
    PlanHistoryEntry(
        id = getLong("id"),
        generatedAt = requireNotNull(optInstant("generated_at")),
        summary = getString("summary"),
        recommendedFocus = optJSONArray("recommended_focus").toStringList(),
        suggestedPace = optNullableString("suggested_pace"),
        executionEffect = optNullableString("execution_effect"),
    )

internal fun JSONObject.toConfusionEdge(): ConfusionEdge =
    ConfusionEdge(
        sourceWordId = getLong("source_word_id"),
        targetWordId = getLong("target_word_id"),
        relationType = getString("relation_type"),
        weight = optDouble("weight", 0.0).toFloat(),
        mistakeCount = optInt("mistake_count", 0),
        updatedAt = optInstant("updated_at") ?: java.time.Instant.EPOCH,
    )

private fun JSONObject.optNullableString(key: String): String? =
    when (val value = opt(key)) {
        null,
        JSONObject.NULL -> null
        else -> value.toString().takeIf(String::isNotBlank)
    }

private fun JSONObject.optIntOrNull(key: String): Int? =
    when (val value = opt(key)) {
        null,
        JSONObject.NULL -> null
        is Number -> value.toInt()
        else -> value.toString().toIntOrNull()
    }

private fun JSONObject.optLongOrNull(key: String): Long? =
    when (val value = opt(key)) {
        null,
        JSONObject.NULL -> null
        is Number -> value.toLong()
        else -> value.toString().toLongOrNull()
    }

private fun JSONObject.optBooleanOrNull(key: String): Boolean? =
    when (val value = opt(key)) {
        null,
        JSONObject.NULL -> null
        is Boolean -> value
        else -> value.toString().toBooleanStrictOrNull()
    }

private fun JSONObject.optInstant(key: String) = optNullableString(key).toBackupInstantOrNull()

private fun JSONArray?.toStringList(): List<String> =
    this?.let { array ->
        List(array.length()) { index -> array.optString(index) }.filter(String::isNotBlank)
    }.orEmpty()

private fun <T> JSONArray?.mapObjects(mapper: JSONObject.() -> T): List<T> =
    this?.let { array ->
        List(array.length()) { index ->
            array.getJSONObject(index).mapper()
        }
    }.orEmpty()
