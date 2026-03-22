package com.yueliangmanle.danci.core.model

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

data class StudyEvent(
    val id: Long = 0,
    val sessionId: Long? = null,
    val wordId: Long,
    val eventType: String,
    val feedback: String? = null,
    val isCorrect: Boolean? = null,
    val happenedAt: Instant,
    val elapsedMillis: Long? = null,
    val metadata: String? = null,
)

object StudyEventType {
    const val CARD_PRESENTED = "card_presented"
    const val CARD_FEEDBACK = "card_feedback"
    const val DETAIL_OPENED = "detail_opened"
    const val QUIZ_STARTED = "quiz_started"
    const val QUIZ_ANSWERED = "quiz_answered"
    const val AI_ACTION = "ai_action"
    const val AUDIO_PLAYED = "audio_played"
}

object StudyEventMetadataKey {
    const val QUEUE_BUCKET = "queue_bucket"
    const val RESPONSE_LATENCY_MS = "response_latency_ms"
    const val SKIPPED = "skipped"
    const val GOAL_SCOPE = "goal_scope"
}

fun studyEventMetadataOf(vararg entries: Pair<String, Any?>): String? =
    entries
        .mapNotNull { (key, value) ->
            val stringValue = value?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            "${encodeStudyEventValue(key)}=${encodeStudyEventValue(stringValue)}"
        }
        .joinToString("&")
        .ifBlank { null }

fun StudyEvent.metadataEntries(): Map<String, String> = parseStudyEventMetadata(metadata)

internal fun parseStudyEventMetadata(metadata: String?): Map<String, String> =
    metadata
        ?.takeIf(String::isNotBlank)
        ?.split("&")
        ?.mapNotNull { entry ->
            val separatorIndex = entry.indexOf('=')
            if (separatorIndex <= 0) {
                return@mapNotNull null
            }
            val key = decodeStudyEventValue(entry.substring(0, separatorIndex))
            val value = decodeStudyEventValue(entry.substring(separatorIndex + 1))
            key.takeIf(String::isNotBlank)?.let { it to value }
        }
        ?.toMap()
        .orEmpty()

private fun encodeStudyEventValue(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

private fun decodeStudyEventValue(value: String): String =
    URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
