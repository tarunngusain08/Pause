package com.pause.app.data.preferences

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fast-read preferences for BootReceiver. Uses device-protected storage so flags
 * are readable before user unlock (Direct Boot / LOCKED_BOOT_COMPLETED).
 */
@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } else {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var anyStrictActive: Boolean
        get() = prefs.getBoolean(KEY_ANY_STRICT_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_ANY_STRICT_ACTIVE, value).apply()

    var parentalActive: Boolean
        get() = prefs.getBoolean(KEY_PARENTAL_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_PARENTAL_ACTIVE, value).apply()

    companion object {
        private const val PREFS_NAME = "pause_session_prefs"
        private const val KEY_ANY_STRICT_ACTIVE = "any_strict_active"
        private const val KEY_PARENTAL_ACTIVE = "parental_active"
    }
}
