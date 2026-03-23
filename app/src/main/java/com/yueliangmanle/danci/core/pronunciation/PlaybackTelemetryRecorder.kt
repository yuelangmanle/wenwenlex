package com.yueliangmanle.danci.core.pronunciation

import com.yueliangmanle.danci.core.data.StudyEventRecorder
import com.yueliangmanle.danci.core.model.PlaybackSource
import com.yueliangmanle.danci.core.model.PronunciationAccent
import com.yueliangmanle.danci.core.model.StudyEvent
import com.yueliangmanle.danci.core.model.StudyEventType
import com.yueliangmanle.danci.core.model.studyEventMetadataOf
import java.time.Instant

class PlaybackTelemetryRecorder(
    private val eventRecorder: StudyEventRecorder,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    fun recordWordPlayback(
        wordId: Long,
        lemma: String,
        normalizedWord: String?,
        source: PlaybackSource,
        accent: PronunciationAccent,
        contextLabel: String,
        success: Boolean,
        cacheHit: Boolean,
        latencyMs: Long,
        voicePackId: String? = null,
        voicePackVersion: String? = null,
        failureStage: String? = null,
        fallbackUsed: Boolean = false,
        preferredSourceId: String? = null,
        actualSourceId: String? = null,
        actualSourceType: String? = null,
        errorMessage: String? = null,
    ) {
        val timestamp = nowProvider()
        eventRecorder.record(
            StudyEvent(
                wordId = wordId,
                eventType = StudyEventType.AUDIO_PLAYED,
                happenedAt = timestamp,
                isCorrect = success,
                metadata = studyEventMetadataOf(
                    "lemma" to lemma,
                    "normalized_word" to normalizedWord,
                    "play_source" to source.storageValue,
                    "resolved_source" to source.storageValue,
                    "play_accent" to accent.storageValue,
                    "play_context" to contextLabel,
                    "play_success" to success,
                    "cache_hit" to cacheHit,
                    "latency_ms" to latencyMs,
                    "voice_pack_id" to voicePackId,
                    "voice_pack_version" to voicePackVersion,
                    "failure_stage" to failureStage,
                    "fallback_used" to fallbackUsed,
                    "preferred_source_id" to preferredSourceId,
                    "actual_source_id" to actualSourceId,
                    "actual_source_type" to actualSourceType,
                    "play_error" to errorMessage,
                    "timestamp" to timestamp.toString(),
                ),
            ),
        )
    }
}
