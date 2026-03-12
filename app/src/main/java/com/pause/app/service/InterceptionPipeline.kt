package com.pause.app.service

import android.content.Context
import com.pause.app.di.PauseAccessibilityEntryPoint
import com.pause.app.service.overlay.OverlayManager

/**
 * Evaluates an app-foreground event through an ordered interception pipeline:
 * Strict -> Content Shield.
 */
class InterceptionPipeline(
    private val overlayManager: OverlayManager,
    private val appEntryPoint: PauseAccessibilityEntryPoint,
    private val isForeground: (String) -> Boolean,
    private val context: Context
) {
    suspend fun evaluate(pkg: String): Boolean {
        return strictStage(pkg)
            || contentShieldStage(pkg)
            || run { overlayManager.dismissBlockIfAllowed(); overlayManager.dismiss(); false }
    }

    private suspend fun strictStage(pkg: String): Boolean {
        val strictSessionManager = appEntryPoint.getStrictSessionManager()
        strictSessionManager.getActiveSession() ?: return false

        return if (strictSessionManager.isPackageBlocked(pkg)) {
            if (!isForeground(pkg)) return true
            overlayManager.showStrictBlockOverlay(
                appName = resolveAppName(pkg),
                remainingMs = strictSessionManager.getRemainingMs(),
                onEmergencyConfirm = { strictSessionManager.confirmEmergencyExit() }
            )
            true
        } else {
            overlayManager.dismissBlockIfAllowed()
            overlayManager.dismiss()
            false
        }
    }

    private suspend fun contentShieldStage(pkg: String): Boolean {
        val contentShieldManager = appEntryPoint.getContentShieldManager()
        if (!contentShieldManager.isPackageBlocked(pkg)) return false
        if (!isForeground(pkg)) return true
        overlayManager.showContentShieldBlockOverlay(resolveAppName(pkg))
        return true
    }

    private fun resolveAppName(pkg: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg
        }
    }
}
