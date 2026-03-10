package com.pause.app.di

import com.pause.app.data.preferences.FeatureFlags
import com.pause.app.data.preferences.PreferencesManager
import com.pause.app.data.repository.AppRepository
import com.pause.app.data.repository.LaunchRepository
import com.pause.app.data.repository.SessionRepository
import com.pause.app.service.AllowanceTracker
import com.pause.app.data.repository.ParentalBlockedAppRepository
import com.pause.app.service.overlay.OverlayManager
import com.pause.app.service.parental.ParentalControlManager
import com.pause.app.service.strict.StrictSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PauseAccessibilityEntryPoint {
    fun getOverlayManager(): OverlayManager
    fun getAppRepository(): AppRepository
    fun getFeatureFlags(): FeatureFlags
    fun getLaunchRepository(): LaunchRepository
    fun getAllowanceTracker(): AllowanceTracker
    fun getSessionRepository(): SessionRepository
    fun getParentalBlockedAppRepository(): ParentalBlockedAppRepository
    fun getPreferencesManager(): PreferencesManager
    fun getStrictSessionManager(): StrictSessionManager
    fun getParentalControlManager(): ParentalControlManager
}
