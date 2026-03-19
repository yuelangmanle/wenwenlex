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
        source: PlaybackSource,
        accent: PronunciationAccent,
        contextLabel: String,
        success: Boolean,
        errorMessage: String? = null,
    ) {
        eventRecorder.record(
            StudyEvent(
                wordId = wordId,
                eventType = StudyEventType.AUDIO_PLAYED,
                happenedAt = nowProvider(),
                isCorrect = success,
                metadata = studyEventMetadataOf(
                    "play_source" to source.storageValue,
                    "play_accent" to accent.storageValue,
                    "play_context" to contextLabel,
                    "play_success" to success,
                    "play_error" to errorMessage,
                ),
            ),
        )
    }
}

