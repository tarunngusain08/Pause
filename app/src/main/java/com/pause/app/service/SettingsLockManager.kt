package com.pause.app.service

import com.pause.app.service.parental.ParentalControlManager
import com.pause.app.service.strict.StrictSessionManager
import javax.inject.Inject
import javax.inject.Singleton

enum class LockReason {
    NONE,
    STRICT_SESSION,
    PARENTAL_CONTROL
}

@Singleton
class SettingsLockManager @Inject constructor(
    private val strictSessionManager: StrictSessionManager,
    private val parentalControlManager: ParentalControlManager
) {

    fun isSettingsLocked(): Boolean =
        strictSessionManager.isSettingsLocked() || parentalControlManager.isActive()

    fun isReadOnly(): Boolean = isSettingsLocked()

    fun getLockReason(): LockReason =
        when {
            strictSessionManager.isSettingsLocked() -> LockReason.STRICT_SESSION
            parentalControlManager.isActive() -> LockReason.PARENTAL_CONTROL
            else -> LockReason.NONE
        }
}
