package com.yueliangmanle.danci.core.model

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
