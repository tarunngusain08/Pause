package com.pause.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    suspend fun getDelayDurationSeconds(): Int =
        dataStore.data.map { it[KEY_DELAY_DURATION] ?: DEFAULT_DELAY_SECONDS }.first()

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

    val parentalSetupStep: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_PARENTAL_SETUP_STEP] ?: 0
    }

    suspend fun setParentalSetupStep(step: Int) {
        dataStore.edit { it[KEY_PARENTAL_SETUP_STEP] = step }
    }

    suspend fun clearParentalSetupStep() {
        dataStore.edit { it.remove(KEY_PARENTAL_SETUP_STEP) }
    }

    val socialMediaFilterEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SOCIAL_MEDIA_FILTER_ENABLED] ?: false
    }

    suspend fun setSocialMediaFilterEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOCIAL_MEDIA_FILTER_ENABLED] = enabled }
    }

    val excludedSocialMediaPackages: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_EXCLUDED_SOCIAL_PACKAGES] ?: setOf("com.whatsapp")
    }

    suspend fun setExcludedSocialMediaPackages(packages: Set<String>) {
        dataStore.edit { it[KEY_EXCLUDED_SOCIAL_PACKAGES] = packages }
    }

    val customSocialMediaPackages: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_SOCIAL_PACKAGES] ?: emptySet()
    }

    suspend fun getCustomSocialMediaPackages(): Set<String> =
        dataStore.data.map { it[KEY_CUSTOM_SOCIAL_PACKAGES] ?: emptySet() }.first()

    suspend fun addCustomSocialMediaPackage(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_SOCIAL_PACKAGES] ?: emptySet()
            prefs[KEY_CUSTOM_SOCIAL_PACKAGES] = current + packageName
        }
    }

    suspend fun removeCustomSocialMediaPackage(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_SOCIAL_PACKAGES] ?: emptySet()
            prefs[KEY_CUSTOM_SOCIAL_PACKAGES] = current - packageName
        }
    }

    // anyStrictActive and parentalActive are managed by SessionPreferences (device-protected
    // SharedPreferences). Do not add duplicate keys here to avoid two sources of truth.

    suspend fun getPinBcryptHash(): String? =
        dataStore.data.map { it[KEY_PIN_BCRYPT_HASH] }.first()

    suspend fun setPinBcryptHash(hash: String?) {
        dataStore.edit {
            if (hash != null) it[KEY_PIN_BCRYPT_HASH] = hash
            else it.remove(KEY_PIN_BCRYPT_HASH)
        }
    }

    suspend fun getRecoveryPhraseHash(): String? =
        dataStore.data.map { it[KEY_RECOVERY_PHRASE_HASH] }.first()

    suspend fun setRecoveryPhraseHash(hash: String?) {
        dataStore.edit {
            if (hash != null) it[KEY_RECOVERY_PHRASE_HASH] = hash
            else it.remove(KEY_RECOVERY_PHRASE_HASH)
        }
    }

    suspend fun getPinAttemptCount(): Int =
        dataStore.data.map { it[KEY_PIN_ATTEMPT_COUNT] ?: 0 }.first()

    suspend fun setPinAttemptCount(count: Int) {
        dataStore.edit { it[KEY_PIN_ATTEMPT_COUNT] = count }
    }

    suspend fun getPinLockoutUntil(): Long =
        dataStore.data.map { it[KEY_PIN_LOCKOUT_UNTIL] ?: 0L }.first()

    suspend fun setPinLockoutUntil(epochMs: Long) {
        dataStore.edit { it[KEY_PIN_LOCKOUT_UNTIL] = epochMs }
    }

    companion object {
        private val KEY_DELAY_DURATION = intPreferencesKey("delay_duration_seconds")
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_CURRENT_PHASE = intPreferencesKey("current_phase")
        private val KEY_LAST_COST_OF_SCROLL_DATE = longPreferencesKey("last_cost_of_scroll_date")
        private val KEY_SHOW_RE_ENTRY_PROMPT = booleanPreferencesKey("show_re_entry_prompt")
        private val KEY_DAILY_ALLOWANCE_MINUTES = intPreferencesKey("daily_allowance_minutes")
        private val KEY_PIN_BCRYPT_HASH = stringPreferencesKey("pin_bcrypt_hash")
        private val KEY_RECOVERY_PHRASE_HASH = stringPreferencesKey("recovery_phrase_hash")
        private val KEY_PIN_ATTEMPT_COUNT = intPreferencesKey("pin_attempt_count")
        private val KEY_PIN_LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
        private val KEY_PARENTAL_SETUP_STEP = intPreferencesKey("parental_setup_step")
        private val KEY_SOCIAL_MEDIA_FILTER_ENABLED = booleanPreferencesKey("social_media_filter_enabled")
        private val KEY_EXCLUDED_SOCIAL_PACKAGES = stringSetPreferencesKey("excluded_social_packages")
        private val KEY_CUSTOM_SOCIAL_PACKAGES = stringSetPreferencesKey("custom_social_packages")

        const val DEFAULT_DELAY_SECONDS = 10
        const val DEFAULT_PHASE = 2
        const val DEFAULT_ALLOWANCE_MINUTES = 60

        const val PHASE_1 = 1
        const val PHASE_2 = 2
        const val PHASE_3 = 3
    }
}
