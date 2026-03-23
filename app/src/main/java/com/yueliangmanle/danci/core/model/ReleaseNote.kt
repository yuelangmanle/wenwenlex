package com.yueliangmanle.danci.core.model

data class ReleaseNote(
    val version: String,
    val date: String,
    val sections: List<ReleaseNoteSection>,
    val isCurrent: Boolean = false,
)

data class ReleaseNoteSection(
    val title: String,
    val items: List<String>,
)
