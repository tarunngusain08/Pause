package com.pause.app.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.pause.app.data.db.entity.ReflectionResponse
import com.pause.app.di.PauseAccessibilityEntryPoint
import com.pause.app.service.overlay.OverlayManager
import com.pause.app.service.webfilter.url.URLClassification
import com.pause.app.service.webfilter.url.URLNormalizer
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
        if (event == null || !::appEntryPoint.isInitialized) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowStateChanged(event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleWindowContentChanged(event)
            else -> { }
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentForegroundPackage) return

        // Lock screen intervention (Phase 3): track transitions to launcher as "unlock" proxy
        val prevPackage = currentForegroundPackage
        currentForegroundPackage = packageName
        if (isLauncherPackage(packageName) && prevPackage != null && !isLauncherPackage(prevPackage)) {
            serviceScope.launch {
                val insights = appEntryPoint.getInsightsRepository()
                insights.recordUnlock()
                val fifteenMinAgo = System.currentTimeMillis() - 15 * 60 * 1000
                val count = insights.getUnlockCountSince(fifteenMinAgo)
                if (count >= 5) {
                    overlayManager.showLockInterventionOverlay(count)
                }
            }
        }

        if (isExcludedPackage(packageName)) return

        serviceScope.launch {
            val pkg = packageName
            val strictSessionManager = appEntryPoint.getStrictSessionManager()
            val parentalControlManager = appEntryPoint.getParentalControlManager()

            // If a strict session is active but this app is NOT blocked, dismiss any
            // lingering strict-block overlay so the user can freely use allowed apps.
            if (strictSessionManager.getActiveSession() != null) {
                if (strictSessionManager.isPackageBlocked(pkg)) {
                    val app = appEntryPoint.getAppRepository().getByPackageName(pkg)
                    val appName = app?.appName ?: pkg
                    if (currentForegroundPackage != pkg) return@launch
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
                    commitmentSession, pkg
                )
            ) {
                val app = appEntryPoint.getAppRepository().getByPackageName(pkg)
                val appName = app?.appName ?: pkg
                if (currentForegroundPackage != pkg) return@launch
                overlayManager.showCommitmentBlockOverlay {
                    serviceScope.launch {
                        appEntryPoint.getSessionRepository().markSessionBroken(
                            commitmentSession.id, System.currentTimeMillis()
                        )
                        if (currentForegroundPackage != pkg) return@launch
                        overlayManager.showCooldownOverlay(
                            durationSeconds = 90,
                            onCancel = { overlayManager.navigateHome() },
                            onComplete = { overlayManager.navigateHome() }
                        )
                    }
                }
                return@launch
            }

            if (parentalControlManager.isAppBlocked(pkg)) {
                val blockedApp = appEntryPoint.getParentalBlockedAppRepository()
                    .getByPackageName(pkg)
                val appName = blockedApp?.appName
                    ?: appEntryPoint.getAppRepository().getByPackageName(pkg)?.appName
                    ?: pkg
                if (currentForegroundPackage != pkg) return@launch
                parentalControlManager.handleAppLaunch(pkg, appName)
                return@launch
            }
            if (parentalControlManager.isAppFrictionRequired(pkg)) {
                val blockedApp = appEntryPoint.getParentalBlockedAppRepository()
                    .getByPackageName(pkg)
                val appName = blockedApp?.appName ?: pkg
                val delaySeconds =
                    appEntryPoint.getPreferencesManager().getDelayDurationSeconds()
                if (currentForegroundPackage != pkg) return@launch
                overlayManager.showDelayOverlay(pkg, appName, delaySeconds)
                return@launch
            }
            if (shouldIntercept(pkg)) {
                val app = appEntryPoint.getAppRepository().getByPackageName(pkg)
                val appName = app?.appName ?: pkg
                val baseDelay =
                    appEntryPoint.getPreferencesManager().getDelayDurationSeconds()
                if (currentForegroundPackage != pkg) return@launch

                // Check daily allowance (Phase 2)
                val phase2Enabled =
                    appEntryPoint.getFeatureFlags().isPhase2Enabled.first()
                if (phase2Enabled &&
                    appEntryPoint.getAllowanceTracker().hasAllowanceReached()
                ) {
                    overlayManager.showAllowanceReachedOverlay(
                        onOpenAnyway = {
                            serviceScope.launch {
                                if (currentForegroundPackage != pkg) return@launch
                                proceedToDelayOrReflection(
                                    pkg, appName, baseDelay, app,
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
                        appEntryPoint.getLaunchRepository().getTodayLaunchCount(pkg)
                    if (todayCount >= limit) {
                        overlayManager.showLaunchLimitOverlay(
                            appName = appName,
                            currentCount = todayCount,
                            limit = limit,
                            onOpenAnyway = {
                                serviceScope.launch {
                                    if (currentForegroundPackage != pkg) return@launch
                                    proceedToDelayOrReflection(
                                        pkg, appName, baseDelay, app,
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
                    pkg, appName, baseDelay, app, overlayManager, appEntryPoint
                )
                return@launch
            }

            // User navigated to an app that is neither blocked nor monitored —
            // dismiss any lingering overlay from a previously blocked app.
            overlayManager.dismiss()
        }
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (!appEntryPoint.getBrowserURLReader().isKnownBrowser(packageName)) return
        val rootNode = rootInActiveWindow ?: return
        val rawUrl = try {
            appEntryPoint.getBrowserURLReader().extractURL(rootNode, packageName)
        } finally {
            rootNode.recycle()
        } ?: return
        serviceScope.launch {
            val config = appEntryPoint.getWebFilterConfigRepository().getConfig()
            if (config == null || !config.urlReaderEnabled) return@launch
            val domain = URLNormalizer.extractDomain(rawUrl) ?: return@launch
            val (classification, _) = appEntryPoint.getURLClassifier().classify(rawUrl, domain)
            appEntryPoint.getURLCaptureQueue().enqueue(
                url = rawUrl,
                domain = domain,
                browserPackage = packageName,
                classification = classification,
                wasBlocked = classification == URLClassification.BLACKLISTED
            )
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

    private fun isLauncherPackage(packageName: String): Boolean =
        packageName in LAUNCHER_PACKAGES

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
        private val LAUNCHER_PACKAGES = setOf(
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher"
        )
    }
}
