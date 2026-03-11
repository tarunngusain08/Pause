package com.pause.app.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.db.entity.ReflectionResponse
import com.pause.app.di.PauseAccessibilityEntryPoint
import com.pause.app.service.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Evaluates an app-foreground event through an ordered interception pipeline:
 * Strict → Commitment → Standard (monitored apps).
 *
 * Each stage either terminates the pipeline (returns true) or falls through.
 */
class InterceptionPipeline(
    private val overlayManager: OverlayManager,
    private val appEntryPoint: PauseAccessibilityEntryPoint,
    private val serviceScope: CoroutineScope,
    private val isForeground: (String) -> Boolean,
    private val context: Context? = null
) {
    /**
     * Evaluate the interception pipeline for the given package.
     * Must be called from [serviceScope] (Dispatchers.Main context).
     */
    suspend fun evaluate(pkg: String): Boolean {
        return strictStage(pkg)
            || commitmentStage(pkg)
            || standardStage(pkg)
            || run { overlayManager.dismiss(); false }
    }

    // ── Stage 1: Strict session ───────────────────────────────────────────────

    private suspend fun strictStage(pkg: String): Boolean {
        val strictSessionManager = appEntryPoint.getStrictSessionManager()
        val session = strictSessionManager.getActiveSession() ?: return false

        return if (strictSessionManager.isPackageBlocked(pkg)) {
            val appName = resolveAppName(pkg)
            if (!isForeground(pkg)) return true
            overlayManager.showStrictBlockOverlay(
                appName = appName,
                remainingMs = strictSessionManager.getRemainingMs(),
                onEmergencyConfirm = { strictSessionManager.confirmEmergencyExit() }
            )
            true
        } else {
            // Active strict session but this app is allowed — clear any lingering block.
            overlayManager.dismiss()
            false
        }
    }

    // ── Stage 2: Commitment session ───────────────────────────────────────────

    private suspend fun commitmentStage(pkg: String): Boolean {
        val sessionRepository = appEntryPoint.getSessionRepository()
        val commitmentSession = sessionRepository.getActiveCommitmentSession() ?: return false
        if (!sessionRepository.isPackageInCommitmentBlockList(commitmentSession, pkg)) return false

        if (!isForeground(pkg)) return true
        overlayManager.showCommitmentBlockOverlay {
            serviceScope.launch {
                sessionRepository.markSessionBroken(
                    commitmentSession.id,
                    System.currentTimeMillis()
                )
                if (!isForeground(pkg)) return@launch
                overlayManager.showCooldownOverlay(
                    durationSeconds = 90,
                    onCancel = { overlayManager.navigateHome() },
                    onComplete = { overlayManager.navigateHome() }
                )
            }
        }
        return true
    }

    // ── Stage 3: Standard monitored-app friction ──────────────────────────────

    private suspend fun standardStage(pkg: String): Boolean {
        if (!appEntryPoint.getAppRepository().isMonitored(pkg)) return false

        val app = appEntryPoint.getAppRepository().getByPackageName(pkg)
        val appName = app?.appName ?: pkg
        val baseDelay = appEntryPoint.getPreferencesManager().getDelayDurationSeconds()
        if (!isForeground(pkg)) return true

        // Show allowance overlay whenever an allowance limit is actually configured.
        if (appEntryPoint.getAllowanceTracker().hasAllowanceReached()) {
            overlayManager.showAllowanceReachedOverlay(
                onOpenAnyway = {
                    serviceScope.launch {
                        if (!isForeground(pkg)) return@launch
                        showDelayOrReflection(pkg, appName, baseDelay, app)
                    }
                },
                onImDone = { }
            )
            return true
        }

        // Check per-app launch limit (Phase 2)
        val limit = app?.dailyLaunchLimit
        if (limit != null) {
            val todayCount = appEntryPoint.getLaunchRepository().getTodayLaunchCount(pkg)
            if (todayCount >= limit) {
                overlayManager.showLaunchLimitOverlay(
                    appName = appName,
                    currentCount = todayCount,
                    limit = limit,
                    onOpenAnyway = {
                        serviceScope.launch {
                            if (!isForeground(pkg)) return@launch
                            showDelayOrReflection(pkg, appName, baseDelay, app)
                        }
                    },
                    onSkip = { }
                )
                return true
            }
        }

        fireHapticFriction()
        showDelayOrReflection(pkg, appName, baseDelay, app)
        return true
    }

    // ── Haptic friction ───────────────────────────────────────────────────────

    private fun fireHapticFriction() {
        val ctx = context ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(VibratorManager::class.java)
                val vibrator = vm?.defaultVibrator ?: return
                val pattern = longArrayOf(0, 80, 60, 80)
                val amps = intArrayOf(0, 180, 0, 120)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Vibrator::class.java) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 80, 60, 80)
                    val amps = intArrayOf(0, 180, 0, 120)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 80, 60, 80), -1)
                }
            }
        } catch (_: Exception) { }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun showDelayOrReflection(
        pkg: String,
        appName: String,
        baseDelay: Int,
        app: MonitoredApp?
    ) {
        if (!isForeground(pkg)) return
        val sessionRepository = appEntryPoint.getSessionRepository()
        val focusSession = sessionRepository.getActiveFocusSession()
        val frictionLevel = app?.frictionLevel ?: MonitoredApp.FrictionLevel.LOW
        val (effectiveDelay, forceReflection) = when {
            focusSession != null -> 20 to true
            frictionLevel == MonitoredApp.FrictionLevel.LOW -> 5 to false
            frictionLevel == MonitoredApp.FrictionLevel.MEDIUM -> 10 to true
            frictionLevel == MonitoredApp.FrictionLevel.HIGH -> 20 to true
            else -> baseDelay to false
        }
        // Show reflection when friction is MEDIUM/HIGH or when a focus session forces it.
        if (forceReflection) {
            overlayManager.showReflectionOverlay(pkg, appName) { reason ->
                serviceScope.launch {
                    val extraDelay = if (reason == ReflectionResponse.REASON_BORED ||
                        reason == ReflectionResponse.REASON_HABIT
                    ) 5 else 0
                    val totalDelay = effectiveDelay + extraDelay
                    if (!isForeground(pkg)) return@launch
                    overlayManager.showDelayOverlay(pkg, appName, totalDelay, reason)
                }
            }
        } else {
            overlayManager.showDelayOverlay(pkg, appName, effectiveDelay)
        }
    }

    private suspend fun resolveAppName(pkg: String): String =
        appEntryPoint.getAppRepository().getByPackageName(pkg)?.appName ?: pkg

}
