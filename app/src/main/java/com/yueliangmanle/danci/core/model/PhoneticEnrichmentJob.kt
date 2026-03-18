package com.yueliangmanle.danci.core.model

import java.time.Instant

data class PhoneticEnrichmentJob(
    val id: Long = 0,
    val scopeType: String,
    val scopeRef: String,
    val profileId: String? = null,
    val fillMode: String,
    val status: String,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
)
