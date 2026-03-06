package com.pause.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pause_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    val delayDurationSeconds: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_DELAY_DURATION] ?: DEFAULT_DELAY_SECONDS
    }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] ?: false
    }

    val currentPhase: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_PHASE] ?: DEFAULT_PHASE
    }

    val lastCostOfScrollShownDate: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_COST_OF_SCROLL_DATE].takeIf { it != 0L }
    }

    val showReEntryPrompt: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SHOW_RE_ENTRY_PROMPT] ?: false
    }

    val dailyAllowanceMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_DAILY_ALLOWANCE_MINUTES] ?: DEFAULT_ALLOWANCE_MINUTES
    }

    suspend fun setDelayDurationSeconds(seconds: Int) {
        dataStore.edit { it[KEY_DELAY_DURATION] = seconds }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setCurrentPhase(phase: Int) {
        dataStore.edit { it[KEY_CURRENT_PHASE] = phase }
    }

    suspend fun setLastCostOfScrollShownDate(dateMillis: Long) {
        dataStore.edit { it[KEY_LAST_COST_OF_SCROLL_DATE] = dateMillis }
    }

    suspend fun setShowReEntryPrompt(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_RE_ENTRY_PROMPT] = show }
    }

    suspend fun setDailyAllowanceMinutes(minutes: Int) {
        dataStore.edit { it[KEY_DAILY_ALLOWANCE_MINUTES] = minutes }
    }

    companion object {
        private val KEY_DELAY_DURATION = intPreferencesKey("delay_duration_seconds")
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_CURRENT_PHASE = intPreferencesKey("current_phase")
        private val KEY_LAST_COST_OF_SCROLL_DATE = longPreferencesKey("last_cost_of_scroll_date")
        private val KEY_SHOW_RE_ENTRY_PROMPT = booleanPreferencesKey("show_re_entry_prompt")
        private val KEY_DAILY_ALLOWANCE_MINUTES = intPreferencesKey("daily_allowance_minutes")

        const val DEFAULT_DELAY_SECONDS = 10
        const val DEFAULT_PHASE = 1
        const val DEFAULT_ALLOWANCE_MINUTES = 60

        const val PHASE_1 = 1
        const val PHASE_2 = 2
        const val PHASE_3 = 3
    }
}
