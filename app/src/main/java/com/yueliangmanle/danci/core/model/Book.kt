package com.yueliangmanle.danci.core.model

import java.time.Instant

data class Book(
    val id: String,
    val title: String,
    val description: String? = null,
    val language: String = "en",
    val category: String = "general",
    val sourceType: String = "builtin",
    val wordCount: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
)
