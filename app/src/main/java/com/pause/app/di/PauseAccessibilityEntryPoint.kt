package com.pause.app.di

import com.pause.app.data.repository.InsightsRepository
import com.pause.app.data.repository.ParentalBlockedAppRepository
import com.pause.app.service.overlay.OverlayManager
import com.pause.app.service.parental.ParentalControlManager
import com.pause.app.service.contentshield.ContentShieldManager
import com.pause.app.service.strict.StrictSessionManager
import com.pause.app.data.repository.WebFilterConfigRepository
import com.pause.app.service.webfilter.url.AutoBlacklistEngine
import com.pause.app.service.webfilter.url.BrowserURLReader
import com.pause.app.service.webfilter.url.URLCaptureQueue
import com.pause.app.service.webfilter.url.URLClassifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PauseAccessibilityEntryPoint {
    fun getOverlayManager(): OverlayManager
    fun getParentalBlockedAppRepository(): ParentalBlockedAppRepository
    fun getStrictSessionManager(): StrictSessionManager
    fun getParentalControlManager(): ParentalControlManager
    fun getContentShieldManager(): ContentShieldManager
    fun getInsightsRepository(): InsightsRepository
    fun getBrowserURLReader(): BrowserURLReader
    fun getURLClassifier(): URLClassifier
    fun getURLCaptureQueue(): URLCaptureQueue
    fun getAutoBlacklistEngine(): AutoBlacklistEngine
    fun getWebFilterConfigRepository(): WebFilterConfigRepository
}
