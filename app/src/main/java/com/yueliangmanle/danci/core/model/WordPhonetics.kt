package com.yueliangmanle.danci.core.model

import java.time.Instant

fun Word.hasAnyPhonetic(): Boolean =
    !phoneticUk.isNullOrBlank() || !phoneticUs.isNullOrBlank() || !phonetic.isNullOrBlank()

fun Word.hasCompletePhonetic(): Boolean =
    !phoneticUk.isNullOrBlank() && !phoneticUs.isNullOrBlank()

fun Word.needsPhoneticFill(): Boolean =
    phoneticUk.isNullOrBlank() || phoneticUs.isNullOrBlank()

fun buildPhoneticStatus(
    phoneticUk: String?,
    phoneticUs: String?,
    fallbackPhonetic: String?,
): String =
    when {
        !phoneticUk.isNullOrBlank() && !phoneticUs.isNullOrBlank() -> PHONETIC_STATUS_COMPLETE
        !fallbackPhonetic.isNullOrBlank() || !phoneticUk.isNullOrBlank() || !phoneticUs.isNullOrBlank() -> PHONETIC_STATUS_PARTIAL
        else -> PHONETIC_STATUS_EMPTY
    }

fun Word.withUpdatedPhonetics(
    phoneticUk: String?,
    phoneticUs: String?,
    source: String,
    updatedAt: Instant,
): Word {
    val fallbackPhonetic = phoneticUk ?: phoneticUs ?: phonetic
    return copy(
        phonetic = fallbackPhonetic,
        phoneticUk = phoneticUk,
        phoneticUs = phoneticUs,
        phoneticSource = source,
        phoneticStatus = buildPhoneticStatus(phoneticUk, phoneticUs, fallbackPhonetic),
        phoneticUpdatedAt = updatedAt,
    )
}
