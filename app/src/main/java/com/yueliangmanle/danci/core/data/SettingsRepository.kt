package com.yueliangmanle.danci.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.yueliangmanle.danci.core.ai.AiRuntimeSettings
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
    val aiEnabled: Boolean = false,
    val aiBaseUrl: String = DEFAULT_AI_BASE_URL,
    val aiModel: String = DEFAULT_AI_MODEL,
    val defaultAiProfileId: String? = null,
    val wordHelpProfileId: String? = null,
    val planAdjustmentProfileId: String? = null,
    val phoneticFillProfileId: String? = null,
    val aiPlanAdjustmentEnabled: Boolean = true,
    val aiSessionCheckpointEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    val reminderMinute: Int = DEFAULT_REMINDER_MINUTE,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun getSettings(): AppSettings

    suspend fun updateDailyGoal(dailyGoal: Int)

    suspend fun updateActiveBookId(bookId: String?)

    suspend fun updateAiEnabled(enabled: Boolean)

    suspend fun updateAiBaseUrl(baseUrl: String)

    suspend fun updateAiModel(model: String)

    suspend fun updateDefaultAiProfileId(profileId: String?)

    suspend fun updateWordHelpProfileId(profileId: String?)

    suspend fun updatePlanAdjustmentProfileId(profileId: String?)

    suspend fun updatePhoneticFillProfileId(profileId: String?)

    suspend fun updateAiPlanAdjustmentEnabled(enabled: Boolean)

    suspend fun updateAiSessionCheckpointEnabled(enabled: Boolean)

    suspend fun updateReminderEnabled(enabled: Boolean)

    suspend fun updateReminderTime(hour: Int, minute: Int)
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "danci_settings",
)

private object SettingsPreferencesKeys {
    val dailyGoal = intPreferencesKey("daily_goal")
    val activeBookId = stringPreferencesKey("active_book_id")
    val aiEnabled = booleanPreferencesKey("ai_enabled")
    val aiBaseUrl = stringPreferencesKey("ai_base_url")
    val aiModel = stringPreferencesKey("ai_model")
    val defaultAiProfileId = stringPreferencesKey("default_ai_profile_id")
    val wordHelpProfileId = stringPreferencesKey("word_help_profile_id")
    val planAdjustmentProfileId = stringPreferencesKey("plan_adjustment_profile_id")
    val phoneticFillProfileId = stringPreferencesKey("phonetic_fill_profile_id")
    val aiPlanAdjustmentEnabled = booleanPreferencesKey("ai_plan_adjustment_enabled")
    val aiSessionCheckpointEnabled = booleanPreferencesKey("ai_session_checkpoint_enabled")
    val reminderEnabled = booleanPreferencesKey("reminder_enabled")
    val reminderHour = intPreferencesKey("reminder_hour")
    val reminderMinute = intPreferencesKey("reminder_minute")
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> =
        dataStore.data.map { preferences ->
            AppSettings(
                dailyGoal = preferences[SettingsPreferencesKeys.dailyGoal] ?: 20,
                activeBookId = preferences[SettingsPreferencesKeys.activeBookId],
                aiEnabled = preferences[SettingsPreferencesKeys.aiEnabled] ?: false,
                aiBaseUrl = preferences[SettingsPreferencesKeys.aiBaseUrl] ?: DEFAULT_AI_BASE_URL,
                aiModel = preferences[SettingsPreferencesKeys.aiModel] ?: DEFAULT_AI_MODEL,
                defaultAiProfileId = preferences[SettingsPreferencesKeys.defaultAiProfileId],
                wordHelpProfileId = preferences[SettingsPreferencesKeys.wordHelpProfileId],
                planAdjustmentProfileId = preferences[SettingsPreferencesKeys.planAdjustmentProfileId],
                phoneticFillProfileId = preferences[SettingsPreferencesKeys.phoneticFillProfileId],
                aiPlanAdjustmentEnabled = preferences[SettingsPreferencesKeys.aiPlanAdjustmentEnabled] ?: true,
                aiSessionCheckpointEnabled = preferences[SettingsPreferencesKeys.aiSessionCheckpointEnabled] ?: true,
                reminderEnabled = preferences[SettingsPreferencesKeys.reminderEnabled] ?: false,
                reminderHour = preferences[SettingsPreferencesKeys.reminderHour] ?: DEFAULT_REMINDER_HOUR,
                reminderMinute = preferences[SettingsPreferencesKeys.reminderMinute] ?: DEFAULT_REMINDER_MINUTE,
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

    override suspend fun updateAiEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.aiEnabled] = enabled
        }
    }

    override suspend fun updateAiBaseUrl(baseUrl: String) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.aiBaseUrl] = baseUrl.ifBlank { DEFAULT_AI_BASE_URL }
        }
    }

    override suspend fun updateAiModel(model: String) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.aiModel] = model.ifBlank { DEFAULT_AI_MODEL }
        }
    }

    override suspend fun updateDefaultAiProfileId(profileId: String?) {
        updateNullableString(SettingsPreferencesKeys.defaultAiProfileId, profileId)
    }

    override suspend fun updateWordHelpProfileId(profileId: String?) {
        updateNullableString(SettingsPreferencesKeys.wordHelpProfileId, profileId)
    }

    override suspend fun updatePlanAdjustmentProfileId(profileId: String?) {
        updateNullableString(SettingsPreferencesKeys.planAdjustmentProfileId, profileId)
    }

    override suspend fun updatePhoneticFillProfileId(profileId: String?) {
        updateNullableString(SettingsPreferencesKeys.phoneticFillProfileId, profileId)
    }

    override suspend fun updateAiPlanAdjustmentEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.aiPlanAdjustmentEnabled] = enabled
        }
    }

    override suspend fun updateAiSessionCheckpointEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.aiSessionCheckpointEnabled] = enabled
        }
    }

    override suspend fun updateReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.reminderEnabled] = enabled
        }
    }

    override suspend fun updateReminderTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.reminderHour] = hour.coerceIn(0, 23)
            preferences[SettingsPreferencesKeys.reminderMinute] = minute.coerceIn(0, 59)
        }
    }

    private suspend fun updateNullableString(
        key: Preferences.Key<String>,
        value: String?,
    ) {
        dataStore.edit { preferences ->
            if (value.isNullOrBlank()) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }
}

fun buildSettingsRepository(context: Context): SettingsRepository =
    DataStoreSettingsRepository(context.settingsDataStore)

fun AppSettings.asAiRuntimeSettings(apiKey: String? = null): AiRuntimeSettings =
    AiRuntimeSettings(
        enabled = aiEnabled,
        baseUrl = aiBaseUrl,
        apiKey = apiKey,
        model = aiModel,
    )

const val DEFAULT_AI_BASE_URL = "https://api.openai.com/v1"
const val DEFAULT_AI_MODEL = "gpt-5-mini"
const val DEFAULT_REMINDER_HOUR = 21
const val DEFAULT_REMINDER_MINUTE = 0
