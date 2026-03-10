package com.pause.app.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.pause.app.data.db.entity.ReflectionResponse
import com.pause.app.di.PauseAccessibilityEntryPoint
import com.pause.app.service.overlay.OverlayManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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

            // If a strict session is active but this app is NOT blocked, dismiss any
            // lingering strict-block overlay so the user can freely use allowed apps.
            if (strictSessionManager.getActiveSession() != null) {
                if (strictSessionManager.isPackageBlocked(packageName)) {
                    val app = appEntryPoint.getAppRepository().getByPackageName(packageName)
                    val appName = app?.appName ?: packageName
                    if (currentForegroundPackage != packageName) return@launch
                    overlayManager.showStrictBlockOverlay(
                        appName = appName,
                        remainingMs = strictSessionManager.getRemainingMs(),
                        onEmergencyConfirm = { strictSessionManager.confirmEmergencyExit() }
                    )
                    return@launch
                } else {
                    overlayManager.dismiss()
                }
            }

            // Commitment mode (Phase 3): block apps in active commitment session
            val commitmentSession = appEntryPoint.getSessionRepository().getActiveCommitmentSession()
            if (commitmentSession != null &&
                appEntryPoint.getSessionRepository().isPackageInCommitmentBlockList(
                    commitmentSession, packageName
                )
            ) {
                val app = appEntryPoint.getAppRepository().getByPackageName(packageName)
                val appName = app?.appName ?: packageName
                if (currentForegroundPackage != packageName) return@launch
                overlayManager.showCommitmentBlockOverlay {
                    serviceScope.launch {
                        appEntryPoint.getSessionRepository().markSessionBroken(
                            commitmentSession.id, System.currentTimeMillis()
                        )
                        if (currentForegroundPackage != packageName) return@launch
                        overlayManager.showCooldownOverlay(
                            durationSeconds = 90,
                            onCancel = { overlayManager.navigateHome() },
                            onComplete = { overlayManager.navigateHome() }
                        )
                    }
                }
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
                val baseDelay =
                    appEntryPoint.getPreferencesManager().getDelayDurationSeconds()
                if (currentForegroundPackage != packageName) return@launch

                // Check daily allowance (Phase 2)
                val phase2Enabled =
                    appEntryPoint.getFeatureFlags().isPhase2Enabled.first()
                if (phase2Enabled &&
                    appEntryPoint.getAllowanceTracker().hasAllowanceReached()
                ) {
                    overlayManager.showAllowanceReachedOverlay(
                        onOpenAnyway = {
                            serviceScope.launch {
                                if (currentForegroundPackage != packageName) return@launch
                                proceedToDelayOrReflection(
                                    packageName, appName, baseDelay, app,
                                    overlayManager, appEntryPoint
                                )
                            }
                        },
                        onImDone = { }
                    )
                    return@launch
                }

                // Check launch limit (Phase 2)
                val limit = app?.dailyLaunchLimit
                if (limit != null) {
                    val todayCount =
                        appEntryPoint.getLaunchRepository().getTodayLaunchCount(packageName)
                    if (todayCount >= limit) {
                        overlayManager.showLaunchLimitOverlay(
                            appName = appName,
                            currentCount = todayCount,
                            limit = limit,
                            onOpenAnyway = {
                                serviceScope.launch {
                                    if (currentForegroundPackage != packageName) return@launch
                                    proceedToDelayOrReflection(
                                        packageName, appName, baseDelay, app,
                                        overlayManager, appEntryPoint
                                    )
                                }
                            },
                            onSkip = { }
                        )
                        return@launch
                    }
                }

                proceedToDelayOrReflection(
                    packageName, appName, baseDelay, app, overlayManager, appEntryPoint
                )
                return@launch
            }

            // User navigated to an app that is neither blocked nor monitored —
            // dismiss any lingering overlay from a previously blocked app.
            overlayManager.dismiss()
        }
    }

    private suspend fun proceedToDelayOrReflection(
        packageName: String,
        appName: String,
        baseDelay: Int,
        app: com.pause.app.data.db.entity.MonitoredApp?,
        overlayManager: OverlayManager,
        appEntryPoint: PauseAccessibilityEntryPoint
    ) {
        if (currentForegroundPackage != packageName) return
        showDelayOrReflection(
            packageName, appName, baseDelay, app, overlayManager, appEntryPoint
        )
    }

    private suspend fun showDelayOrReflection(
        packageName: String,
        appName: String,
        baseDelay: Int,
        app: com.pause.app.data.db.entity.MonitoredApp?,
        overlayManager: OverlayManager,
        appEntryPoint: PauseAccessibilityEntryPoint
    ) {
        if (currentForegroundPackage != packageName) return
        val focusSession = appEntryPoint.getSessionRepository().getActiveFocusSession()
        val frictionLevel = app?.frictionLevel ?: com.pause.app.data.db.entity.MonitoredApp.FrictionLevel.LOW
        val (effectiveDelay, forceReflection) = when {
            focusSession != null -> 20 to true
            frictionLevel == com.pause.app.data.db.entity.MonitoredApp.FrictionLevel.LOW -> 5 to false
            frictionLevel == com.pause.app.data.db.entity.MonitoredApp.FrictionLevel.MEDIUM -> 10 to true
            frictionLevel == com.pause.app.data.db.entity.MonitoredApp.FrictionLevel.HIGH -> 20 to true
            else -> baseDelay to false
        }
        val phase2Enabled = appEntryPoint.getFeatureFlags().isPhase2Enabled.first()
        if (phase2Enabled || forceReflection) {
            overlayManager.showReflectionOverlay(packageName, appName) { reason ->
                serviceScope.launch {
                    val extraDelay = if (reason == ReflectionResponse.REASON_BORED ||
                        reason == ReflectionResponse.REASON_HABIT
                    ) 5 else 0
                    val totalDelay = effectiveDelay + extraDelay
                    if (currentForegroundPackage != packageName) return@launch
                    overlayManager.showDelayOverlay(
                        packageName, appName, totalDelay, reason
                    )
                }
            }
        } else {
            overlayManager.showDelayOverlay(packageName, appName, effectiveDelay)
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
