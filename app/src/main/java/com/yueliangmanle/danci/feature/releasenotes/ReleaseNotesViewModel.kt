package com.yueliangmanle.danci.feature.releasenotes

import android.content.Context
import com.yueliangmanle.danci.BuildConfig
import com.yueliangmanle.danci.core.model.ReleaseNote
import com.yueliangmanle.danci.core.releasenotes.ReleaseNotesCatalog
import com.yueliangmanle.danci.core.releasenotes.buildReleaseNotesCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ReleaseNotesUiState(
    val isLoading: Boolean = false,
    val currentVersion: String = "",
    val notes: List<ReleaseNote> = emptyList(),
    val errorMessage: String? = null,
)

class ReleaseNotesViewModel(
    private val catalog: ReleaseNotesCatalog,
    private val versionName: String,
) {
    suspend fun loadUiState(): ReleaseNotesUiState = withContext(Dispatchers.IO) {
        ReleaseNotesUiState(
            currentVersion = versionName,
            notes = catalog.load(currentVersion = versionName),
        )
    }
}

suspend fun loadReleaseNotesViewModel(
    context: Context,
): ReleaseNotesViewModel = withContext(Dispatchers.IO) {
    ReleaseNotesViewModel(
        catalog = buildReleaseNotesCatalog(context.applicationContext),
        versionName = BuildConfig.VERSION_NAME,
    )
}
