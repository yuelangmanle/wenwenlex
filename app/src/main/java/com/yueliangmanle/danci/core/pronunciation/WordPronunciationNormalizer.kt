package com.yueliangmanle.danci.core.pronunciation

private val SUPPORTED_WORD_PATTERN = Regex("[a-z][a-z'\\-]*")

fun normalizeWordForPronunciation(raw: String): String? =
    raw.trim()
        .replace('’', '\'')
        .lowercase()
        .takeIf { it.matches(SUPPORTED_WORD_PATTERN) }
