package com.yueliangmanle.danci.core.backup

import com.yueliangmanle.danci.core.data.AppSettings
import com.yueliangmanle.danci.core.database.entity.AiProviderProfileEntity
import com.yueliangmanle.danci.core.database.entity.AudioGenerationTaskEntity
import com.yueliangmanle.danci.core.database.entity.AudioGenerationTaskItemEntity
import com.yueliangmanle.danci.core.database.entity.BookEntity
import com.yueliangmanle.danci.core.database.entity.BookWordEntity
import com.yueliangmanle.danci.core.database.entity.ImportBatchEntity
import com.yueliangmanle.danci.core.database.entity.LearningRecordEntity
import com.yueliangmanle.danci.core.database.entity.PhoneticEnrichmentJobEntity
import com.yueliangmanle.danci.core.database.entity.PronunciationSourceEntity
import com.yueliangmanle.danci.core.database.entity.PronunciationSourcePresetEntity
import com.yueliangmanle.danci.core.database.entity.StudyEventEntity
import com.yueliangmanle.danci.core.database.entity.StudySessionEntity
import com.yueliangmanle.danci.core.database.entity.VoicePackEntity
import com.yueliangmanle.danci.core.database.entity.WordEntity
import com.yueliangmanle.danci.core.database.entity.WordAudioAssetEntity
import com.yueliangmanle.danci.core.model.AiMemorySummary
import java.time.Instant

const val BACKUP_VERSION = 7
const val MANIFEST_FILE_NAME = "manifest.json"
const val PAYLOAD_FILE_NAME = "payload.json"
val SUPPORTED_BACKUP_VERSIONS = setOf(1, 2, 3, 4, 5, 6, BACKUP_VERSION)

val REQUIRED_BACKUP_SECTIONS = listOf(
    "settings",
    "learner_profile",
    "daily_summary",
    "weekly_summary",
    "plan_history",
    "confusion_graph",
)

data class BackupManifest(
    val version: Int,
    val createdAt: String,
    val sections: List<String>,
)

data class BackupSnapshot(
    val settings: AppSettings,
    val aiProfiles: List<AiProviderProfileEntity> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val bookWords: List<BookWordEntity> = emptyList(),
    val words: List<WordEntity> = emptyList(),
    val wordAudioAssets: List<WordAudioAssetEntity> = emptyList(),
    val pronunciationSources: List<PronunciationSourceEntity> = emptyList(),
    val pronunciationSourcePresets: List<PronunciationSourcePresetEntity> = emptyList(),
    val audioGenerationTasks: List<AudioGenerationTaskEntity> = emptyList(),
    val audioGenerationTaskItems: List<AudioGenerationTaskItemEntity> = emptyList(),
    val voicePacks: List<VoicePackEntity> = emptyList(),
    val importBatches: List<ImportBatchEntity> = emptyList(),
    val phoneticEnrichmentJobs: List<PhoneticEnrichmentJobEntity> = emptyList(),
    val learningRecords: List<LearningRecordEntity> = emptyList(),
    val studySessions: List<StudySessionEntity> = emptyList(),
    val studyEvents: List<StudyEventEntity> = emptyList(),
    val aiMemorySummary: AiMemorySummary = AiMemorySummary(),
)

data class BackupArchive(
    val manifest: BackupManifest,
    val serializedJson: String,
    val zippedBytes: ByteArray,
)

data class ImportedBackup(
    val manifest: BackupManifest,
    val snapshot: BackupSnapshot,
)

internal fun Instant?.toBackupString(): String? = this?.toString()

internal fun String?.toBackupInstantOrNull(): Instant? =
    this?.takeIf(String::isNotBlank)?.let(Instant::parse)
