package com.pause.app.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.pause.app.di.PauseAccessibilityEntryPoint
import com.pause.app.service.overlay.OverlayManager
import com.pause.app.service.webfilter.url.URLClassification
import com.pause.app.service.webfilter.url.URLNormalizer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PauseAccessibilityService : AccessibilityService() {

    @Volatile
    private var foregroundPackage: String? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lastExtractTime = LinkedHashMap<String, Long>(16, 0.75f, true)

    private lateinit var overlayManager: OverlayManager
    private lateinit var appEntryPoint: PauseAccessibilityEntryPoint
    private lateinit var pipeline: InterceptionPipeline

    private val dynamicExcludedPackages = mutableSetOf<String>()

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_USER_PRESENT) return
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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        appEntryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PauseAccessibilityEntryPoint::class.java
        )
        overlayManager = appEntryPoint.getOverlayManager()
        val launcherPkg = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        if (launcherPkg != null) dynamicExcludedPackages.add(launcherPkg)

        pipeline = InterceptionPipeline(
            overlayManager = overlayManager,
            appEntryPoint = appEntryPoint,
            context = applicationContext,
            isForeground = { pkg -> foregroundPackage == pkg }
        )
        registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (!::appEntryPoint.isInitialized) return super.onKeyEvent(event)
        val strictSessionManager = appEntryPoint.getStrictSessionManager()

        // Block recents tray when a strict block overlay is active
        if (event?.keyCode == KeyEvent.KEYCODE_APP_SWITCH && event.action == KeyEvent.ACTION_DOWN) {
            val state = overlayManager.getState()
            if (state == com.pause.app.service.overlay.OverlayState.SHOWING_STRICT_BLOCK ||
                state == com.pause.app.service.overlay.OverlayState.SHOWING_CONTENT_SHIELD_BLOCK) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                return true
            }
        }

        // Block back key when Content Shield or Strict Block overlay is active
        if (event?.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            val state = overlayManager.getState()
            if (state == com.pause.app.service.overlay.OverlayState.SHOWING_CONTENT_SHIELD_BLOCK ||
                state == com.pause.app.service.overlay.OverlayState.SHOWING_STRICT_BLOCK) {
                return true
            }
        }

        // When strict block overlay is showing, also suppress the Home key on devices where it
        // is accessible via accessibility key events (not universal but helps on some OEMs).
        if (event?.keyCode == KeyEvent.KEYCODE_HOME && event.action == KeyEvent.ACTION_DOWN) {
            val state = overlayManager.getState()
            if (state == com.pause.app.service.overlay.OverlayState.SHOWING_STRICT_BLOCK) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                return true
            }
        }

        if (event?.keyCode == KeyEvent.KEYCODE_POWER &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.isLongPress
        ) {
            if (strictSessionManager.getActiveSession() != null) {
                overlayManager.showPowerMenuBlockOverlay(strictSessionManager.getRemainingMs())
                return true
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
        val state = overlayManager.getState()
        val isBlockOverlayActive = state == com.pause.app.service.overlay.OverlayState.SHOWING_CONTENT_SHIELD_BLOCK ||
            state == com.pause.app.service.overlay.OverlayState.SHOWING_STRICT_BLOCK
        if (packageName == foregroundPackage && !isBlockOverlayActive) return

        foregroundPackage = packageName

        if (isExcludedPackage(packageName)) {
            overlayManager.dismissBlockIfAllowed()
            overlayManager.dismiss()
            return
        }

        // When focus mode is active and the launcher comes to the foreground (Home button pressed),
        // immediately bring MainActivity back to the front to prevent the user from escaping.
        if (isLauncherPackage(packageName)) {
            val strictSession = appEntryPoint.getStrictSessionManager().getActiveSession()
            if (strictSession != null) {
                val intent = Intent(applicationContext, com.pause.app.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                applicationContext.startActivity(intent)
                return
            }
            overlayManager.dismissBlockIfAllowed()
            overlayManager.dismiss()
            return
        }

        serviceScope.launch {
            pipeline.evaluate(packageName)
        }
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (!appEntryPoint.getBrowserURLReader().isKnownBrowser(packageName)) return
        val now = System.currentTimeMillis()
        if (now - (lastExtractTime[packageName] ?: 0L) < CONTENT_CHANGED_DEBOUNCE_MS) return
        lastExtractTime[packageName] = now
        if (lastExtractTime.size > MAX_EXTRACT_TIME_ENTRIES) {
            lastExtractTime.remove(lastExtractTime.keys.first())
        }
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
            val (classification, keywordMatch) = appEntryPoint.getURLClassifier().classify(rawUrl, domain)
            appEntryPoint.getURLCaptureQueue().enqueue(
                url = rawUrl,
                domain = domain,
                browserPackage = packageName,
                classification = classification,
                wasBlocked = classification == URLClassification.BLACKLISTED
            )
            if (classification == URLClassification.KEYWORD_MATCH && keywordMatch != null) {
                appEntryPoint.getAutoBlacklistEngine()
                    .onKeywordMatch(domain, keywordMatch.keyword)
            }
            if (classification == URLClassification.BLACKLISTED ||
                classification == URLClassification.KEYWORD_MATCH
            ) {
                overlayManager.showContentShieldBlockOverlay(domain)
            }
        }
    }

    private fun isExcludedPackage(packageName: String): Boolean {
        if (packageName == applicationContext.packageName) return true
        if (packageName in dynamicExcludedPackages) return true
        return packageName in EXCLUDED_PACKAGES
    }

    private fun isLauncherPackage(packageName: String): Boolean =
        packageName in dynamicExcludedPackages || packageName in LAUNCHER_PACKAGES

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        try { unregisterReceiver(userPresentReceiver) } catch (_: IllegalArgumentException) { }
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    companion object {
        private const val CONTENT_CHANGED_DEBOUNCE_MS = 500L
        private const val MAX_EXTRACT_TIME_ENTRIES = 20

        private val LAUNCHER_PACKAGES = setOf(
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher"
        )
        private val EXCLUDED_PACKAGES = LAUNCHER_PACKAGES + setOf(
            "com.android.systemui",
            "com.android.packageinstaller"
        )
    }
}
