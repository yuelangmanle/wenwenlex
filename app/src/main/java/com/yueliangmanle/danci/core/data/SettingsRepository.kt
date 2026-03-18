package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettings(
    val dailyGoal: Int = 20,
    val activeBookId: String? = null,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun getSettings(): AppSettings

    suspend fun updateDailyGoal(dailyGoal: Int)

    suspend fun updateActiveBookId(bookId: String?)
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "danci_settings",
)

private object SettingsPreferencesKeys {
    val dailyGoal = intPreferencesKey("daily_goal")
    val activeBookId = stringPreferencesKey("active_book_id")
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> =
        dataStore.data.map { preferences ->
            AppSettings(
                dailyGoal = preferences[SettingsPreferencesKeys.dailyGoal] ?: 20,
                activeBookId = preferences[SettingsPreferencesKeys.activeBookId],
            )
        }

    override suspend fun getSettings(): AppSettings = settings.first()

    override suspend fun updateDailyGoal(dailyGoal: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.dailyGoal] = dailyGoal.coerceAtLeast(1)
        }
    }

    override suspend fun updateActiveBookId(bookId: String?) {
        dataStore.edit { preferences ->
            if (bookId.isNullOrBlank()) {
                preferences.remove(SettingsPreferencesKeys.activeBookId)
            } else {
                preferences[SettingsPreferencesKeys.activeBookId] = bookId
            }
        }
    }
}

fun buildSettingsRepository(context: Context): SettingsRepository =
    DataStoreSettingsRepository(context.settingsDataStore)
