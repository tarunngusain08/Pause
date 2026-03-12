package com.pause.app.service

import android.content.Context
import android.content.pm.PackageManager
import com.pause.app.di.PauseAccessibilityEntryPoint
import com.pause.app.service.overlay.OverlayManager

/**
 * Evaluates an app-foreground event through an ordered interception pipeline:
 * Strict -> Content Shield.
 */
class InterceptionPipeline(
    private val overlayManager: OverlayManager,
    private val appEntryPoint: PauseAccessibilityEntryPoint,
    private val context: Context,
    private val isForeground: (String) -> Boolean
) {
    suspend fun evaluate(pkg: String) {
        if (strictStage(pkg)) return
        if (contentShieldStage(pkg)) return
        overlayManager.dismissBlockIfAllowed()
        overlayManager.dismiss()
    }

    private suspend fun strictStage(pkg: String): Boolean {
        val strictSessionManager = appEntryPoint.getStrictSessionManager()
        strictSessionManager.getActiveSession() ?: return false

        return if (strictSessionManager.isPackageBlocked(pkg)) {
            if (!isForeground(pkg)) return false
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
        if (!isForeground(pkg)) return false
        overlayManager.showContentShieldBlockOverlay(resolveAppName(pkg))
        return true
    }

    private fun resolveAppName(pkg: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }
    }
}
