package com.yueliangmanle.danci.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "ai_provider_profiles")
data class AiProviderProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val providerType: String = "custom",
    val baseUrl: String,
    val model: String,
    val enabled: Boolean = true,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val lastValidatedAt: Instant? = null,
)
