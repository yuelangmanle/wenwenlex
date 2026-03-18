package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "phonetic_enrichment_jobs")
data class PhoneticEnrichmentJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
