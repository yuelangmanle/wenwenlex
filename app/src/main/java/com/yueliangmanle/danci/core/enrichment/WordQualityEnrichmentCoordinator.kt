package com.yueliangmanle.danci.core.enrichment

import com.yueliangmanle.danci.core.data.BookRepository
import com.yueliangmanle.danci.core.data.PhoneticEnrichmentRepository
import com.yueliangmanle.danci.core.model.PhoneticEnrichmentJob
import java.time.Instant

enum class QualityFillDimension(
    val storageValue: String,
    val label: String,
) {
    PHONETIC("phonetic", "音标"),
    SYNONYM("synonym", "近义词"),
    ANTONYM("antonym", "反义词"),
    SIMILAR("similar", "拼写相近词"),
    EXAMPLE("example", "例句"),
    WORD_FORM("word_form", "词形变化"),
}

class WordQualityEnrichmentCoordinator(
    private val repository: PhoneticEnrichmentRepository,
    private val bookRepository: BookRepository,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    fun defaultDimensions(): Set<QualityFillDimension> =
        linkedSetOf(
            QualityFillDimension.PHONETIC,
            QualityFillDimension.SYNONYM,
            QualityFillDimension.ANTONYM,
            QualityFillDimension.SIMILAR,
            QualityFillDimension.EXAMPLE,
            QualityFillDimension.WORD_FORM,
        )

    fun createBookJob(
        bookId: String,
        dimensions: Set<QualityFillDimension>,
        totalCount: Int,
        profileId: String? = null,
    ): PhoneticEnrichmentJob {
        val now = nowProvider()
        return PhoneticEnrichmentJob(
            scopeType = "book",
            scopeRef = bookId,
            profileId = profileId,
            fillMode = dimensions.toStorageValue(),
            status = "queued",
            totalCount = totalCount,
            createdAt = now,
            updatedAt = now,
        )
    }

    suspend fun enqueueBookEnrichment(
        bookId: String,
        dimensions: Set<QualityFillDimension>,
        profileId: String? = null,
    ): Long {
        val totalCount = bookRepository.getWords(bookId).size
        return repository.insert(
            createBookJob(
                bookId = bookId,
                dimensions = dimensions,
                totalCount = totalCount,
                profileId = profileId,
            ),
        )
    }

    fun parseDimensions(fillMode: String): Set<QualityFillDimension> =
        fillMode.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .mapNotNull { token ->
                QualityFillDimension.entries.firstOrNull { it.storageValue == token }
            }
            .toCollection(linkedSetOf())

    fun describeDimensions(fillMode: String): String {
        val dimensions = parseDimensions(fillMode).ifEmpty { defaultDimensions() }
        return dimensions.joinToString("、") { it.label }
    }
}

private fun Set<QualityFillDimension>.toStorageValue(): String =
    asSequence()
        .sortedBy(QualityFillDimension::storageValue)
        .joinToString(",") { it.storageValue }
