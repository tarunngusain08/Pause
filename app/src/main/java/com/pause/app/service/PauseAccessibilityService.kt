package com.pause.app.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.pause.app.di.PauseAccessibilityEntryPoint
import com.pause.app.service.overlay.OverlayManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PauseAccessibilityService : AccessibilityService() {

    private var currentForegroundPackage: String? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var overlayManager: OverlayManager
    private lateinit var appEntryPoint: PauseAccessibilityEntryPoint

    override fun onServiceConnected() {
        super.onServiceConnected()
        appEntryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PauseAccessibilityEntryPoint::class.java
        )
        overlayManager = appEntryPoint.getOverlayManager()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (!::appEntryPoint.isInitialized) return super.onKeyEvent(event)
        val strictSessionManager = appEntryPoint.getStrictSessionManager()
        val parentalControlManager = appEntryPoint.getParentalControlManager()
        if (event?.keyCode == KeyEvent.KEYCODE_POWER &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.isLongPress
        ) {
            when {
                strictSessionManager.getActiveSession() != null -> {
                    overlayManager.showPowerMenuBlockOverlay(strictSessionManager.getRemainingMs())
                    return true
                }
                parentalControlManager.isActive() -> {
                    serviceScope.launch {
                        val remaining = parentalControlManager.getTimeUntilNextBandChange()
                        overlayManager.showPowerMenuBlockOverlay(remaining)
                    }
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!::appEntryPoint.isInitialized) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentForegroundPackage) return
        currentForegroundPackage = packageName

        if (isExcludedPackage(packageName)) return

        serviceScope.launch {
            val strictSessionManager = appEntryPoint.getStrictSessionManager()
            val parentalControlManager = appEntryPoint.getParentalControlManager()
            if (strictSessionManager.getActiveSession() != null &&
                strictSessionManager.isPackageBlocked(packageName)
            ) {
                val app = appEntryPoint.getAppRepository().getByPackageName(packageName)
                val appName = app?.appName ?: packageName
                if (currentForegroundPackage != packageName) return@launch
                overlayManager.showStrictBlockOverlay(
                    appName = appName,
                    remainingMs = strictSessionManager.getRemainingMs(),
                    onEmergencyConfirm = { strictSessionManager.confirmEmergencyExit() }
                )
                return@launch
            }
            if (parentalControlManager.isAppBlocked(packageName)) {
                val blockedApp = appEntryPoint.getParentalBlockedAppRepository()
                    .getByPackageName(packageName)
                val appName = blockedApp?.appName
                    ?: appEntryPoint.getAppRepository().getByPackageName(packageName)?.appName
                    ?: packageName
                if (currentForegroundPackage != packageName) return@launch
                parentalControlManager.handleAppLaunch(packageName, appName)
                return@launch
            }
            if (parentalControlManager.isAppFrictionRequired(packageName)) {
                val blockedApp = appEntryPoint.getParentalBlockedAppRepository()
                    .getByPackageName(packageName)
                val appName = blockedApp?.appName ?: packageName
                val delaySeconds =
                    appEntryPoint.getPreferencesManager().getDelayDurationSeconds()
                if (currentForegroundPackage != packageName) return@launch
                overlayManager.showDelayOverlay(packageName, appName, delaySeconds)
                return@launch
            }
            if (shouldIntercept(packageName)) {
                val app = appEntryPoint.getAppRepository().getByPackageName(packageName)
                val appName = app?.appName ?: packageName
                val delaySeconds =
                    appEntryPoint.getPreferencesManager().getDelayDurationSeconds()
                if (currentForegroundPackage != packageName) return@launch
                overlayManager.showDelayOverlay(packageName, appName, delaySeconds)
            }
        }
    }

    private fun isExcludedPackage(packageName: String): Boolean {
        if (packageName == applicationContext.packageName) return true
        return packageName in EXCLUDED_PACKAGES
    }

    private suspend fun shouldIntercept(packageName: String): Boolean =
        appEntryPoint.getAppRepository().isMonitored(packageName)

    override fun onInterrupt() {}

    companion object {
        private val EXCLUDED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.android.settings",
            "com.android.packageinstaller"
        )
    }
}
