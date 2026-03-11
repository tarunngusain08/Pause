package com.pause.app.service.overlay

import androidx.annotation.MainThread
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.pause.app.data.db.entity.LaunchEvent
import com.pause.app.data.repository.LaunchRepository
import com.pause.app.service.strict.EmergencyExitController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val windowManager: WindowManager,
    private val launchRepository: LaunchRepository,
    private val emergencyExitController: EmergencyExitController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentOverlay: View? = null
    private var currentState: OverlayState = OverlayState.IDLE

    @MainThread
    fun getState(): OverlayState = currentState

    /** Returns true if a new overlay with [newState] should preempt the current one. */
    @MainThread
    private fun canShow(newState: OverlayState): Boolean {
        if (newState.priority > currentState.priority) {
            dismissOverlay()
            return true
        }
        return currentState == OverlayState.IDLE
    }

    @MainThread
    fun showReflectionOverlay(
        @Suppress("UNUSED_PARAMETER") packageName: String,
        appName: String,
        onReasonSelected: (String) -> Unit
    ) {
        if (!canShow(OverlayState.SHOWING_REFLECTION)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission not granted, skipping reflection overlay")
            return
        }
        currentState = OverlayState.SHOWING_REFLECTION
        val overlay = ReflectionOverlayView(context, appName) { reason ->
            dismissOverlay()
            onReasonSelected(reason)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try {
            currentOverlay = overlay
            windowManager.addView(overlay, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach reflection overlay", e)
            currentOverlay = null
            currentState = OverlayState.IDLE
        }
    }

    @MainThread
    fun showDelayOverlay(
        packageName: String,
        appName: String,
        delaySeconds: Int,
        reflectionReason: String? = null
    ) {
        if (!canShow(OverlayState.SHOWING_DELAY)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Overlay permission not granted, skipping overlay")
            return
        }

        currentState = OverlayState.SHOWING_DELAY

        val overlay = DelayOverlayView(context).apply {
            setOnCancelListener {
                scope.launch {
                    launchRepository.recordLaunch(
                        LaunchEvent(
                            packageName = packageName,
                            launchedAt = System.currentTimeMillis(),
                            wasCancelled = true,
                            delayDurationSeconds = delaySeconds,
                            reflectionReason = reflectionReason
                        )
                    )
                    dismissOverlay()
                    navigateHome()
                }
            }
            setOnCompleteListener {
                scope.launch {
                    launchRepository.recordLaunch(
                        LaunchEvent(
                            packageName = packageName,
                            launchedAt = System.currentTimeMillis(),
                            wasCancelled = false,
                            delayDurationSeconds = delaySeconds,
                            reflectionReason = reflectionReason
                        )
                    )
                    dismissOverlay()
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            currentOverlay = overlay
            windowManager.addView(overlay, params)
            overlay.show(appName, delaySeconds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay", e)
            currentOverlay = null
            currentState = OverlayState.IDLE
        }
    }

    /**
     * Dismiss overlays that are triggered by app interception (block/delay).
     * Session-complete and session-resume overlays are NOT dismissed here because
     * they are user-facing informational dialogs that the user should dismiss.
     */
    @MainThread
    fun dismiss() {
        val dismissable = setOf(
            OverlayState.SHOWING_STRICT_BLOCK,
            OverlayState.SHOWING_DELAY,
            OverlayState.SHOWING_LAUNCH_LIMIT,
            OverlayState.SHOWING_ALLOWANCE_REACHED,
            OverlayState.SHOWING_COOLDOWN,
            OverlayState.SHOWING_PARENTAL_BLOCK,
            OverlayState.SHOWING_POWER_BLOCK,
            OverlayState.SHOWING_EMERGENCY_CONFIRM,
            OverlayState.SHOWING_REFLECTION,
            OverlayState.SHOWING_COMMITMENT_BLOCK,
            OverlayState.SHOWING_LOCK_INTERVENTION,
            OverlayState.SHOWING_PIN_ENTRY
        )
        if (currentState in dismissable) {
            dismissOverlay()
        }
    }

    @MainThread
    fun showStrictBlockOverlay(
        appName: String,
        remainingMs: Long,
        onEmergencyConfirm: () -> Unit
    ) {
        if (!canShow(OverlayState.SHOWING_STRICT_BLOCK) &&
            currentState != OverlayState.SHOWING_STRICT_BLOCK) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        dismissOverlay()
        currentState = OverlayState.SHOWING_STRICT_BLOCK
        emergencyExitController.reset()

        val overlay = StrictBlockOverlayView(
            context = context,
            appName = appName,
            remainingMs = remainingMs,
            emergencyExitController = emergencyExitController,
            onGoBack = {
                dismissOverlay()
                navigateHome()
            },
            onShowConfirmation = {
                showEmergencyConfirmOverlay(
                    onConfirm = { dismissOverlay(); onEmergencyConfirm() },
                    onCancel = { dismissOverlay() }
                )
            }
        )
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showPowerMenuBlockOverlay(remainingMs: Long) {
        if (!canShow(OverlayState.SHOWING_POWER_BLOCK)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_POWER_BLOCK
        val overlay = PowerMenuBlockOverlayView(context, remainingMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showSessionResumeOverlay(remainingMs: Long) {
        if (!canShow(OverlayState.SHOWING_SESSION_RESUME)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_SESSION_RESUME
        val overlay = SessionResumeOverlayView(context, remainingMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showSessionCompleteOverlay(durationMs: Long) {
        if (!canShow(OverlayState.SHOWING_SESSION_COMPLETE)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_SESSION_COMPLETE
        val overlay = SessionCompleteOverlayView(context, durationMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showEmergencyConfirmOverlay(onConfirm: () -> Unit, onCancel: () -> Unit) {
        if (!canShow(OverlayState.SHOWING_EMERGENCY_CONFIRM)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_EMERGENCY_CONFIRM
        val overlay = EmergencyConfirmOverlayView(context, onConfirm, onCancel)
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showParentalBlockOverlay(
        appName: String,
        liftsAt: String,
        emergencyContact: String?,
        onEmergencyContact: () -> Unit = {},
        onDismiss: () -> Unit = { dismissOverlay() }
    ) {
        if (!canShow(OverlayState.SHOWING_PARENTAL_BLOCK) &&
            currentState != OverlayState.SHOWING_PARENTAL_BLOCK) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        dismissOverlay()
        currentState = OverlayState.SHOWING_PARENTAL_BLOCK
        val overlay = ParentalBlockOverlayView(
            context = context,
            appName = appName,
            liftsAt = liftsAt,
            emergencyContactName = emergencyContact,
            onEmergencyContact = onEmergencyContact,
            onDismiss = onDismiss
        )
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showPINEntryOverlay(
        onSuccess: (String) -> Unit,
        onForgotPIN: () -> Unit,
        onPinAttempt: ((String, (String?) -> Unit) -> Unit)? = null
    ) {
        if (!canShow(OverlayState.SHOWING_PIN_ENTRY)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return
        currentState = OverlayState.SHOWING_PIN_ENTRY
        var pinOverlayRef: PINEntryOverlayView? = null
        val overlay = PINEntryOverlayView(
            context = context,
            onSuccess = { pin ->
                if (onPinAttempt != null) {
                    // Let caller verify the PIN; callback receives an error message or null on success
                    onPinAttempt(pin) { errorMsg ->
                        if (errorMsg == null) {
                            dismissOverlay()
                            onSuccess(pin)
                        } else {
                            pinOverlayRef?.showError(errorMsg)
                        }
                    }
                } else {
                    dismissOverlay()
                    onSuccess(pin)
                }
            },
            onForgotPIN = { dismissOverlay(); onForgotPIN() }
        )
        pinOverlayRef = overlay
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showAllowanceReachedOverlay(
        onOpenAnyway: () -> Unit,
        onImDone: () -> Unit
    ) {
        if (!canShow(OverlayState.SHOWING_ALLOWANCE_REACHED)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_ALLOWANCE_REACHED
        val overlay = AllowanceReachedOverlayView(
            context = context,
            onOpenAnyway = {
                dismissOverlay()
                onOpenAnyway()
            },
            onImDone = {
                dismissOverlay()
                navigateHome()
                onImDone()
            }
        )
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showCommitmentBlockOverlay(onBreakCommitment: () -> Unit) {
        if (!canShow(OverlayState.SHOWING_COMMITMENT_BLOCK)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_COMMITMENT_BLOCK
        val overlay = CommitmentBlockOverlayView(context) {
            dismissOverlay()
            onBreakCommitment()
        }
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showCooldownOverlay(
        durationSeconds: Int,
        onCancel: () -> Unit,
        onComplete: () -> Unit
    ) {
        if (!canShow(OverlayState.SHOWING_COOLDOWN)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_COOLDOWN
        val overlay = CooldownOverlayView(
            context = context,
            durationSeconds = durationSeconds,
            onCancel = {
                dismissOverlay()
                onCancel()
            },
            onComplete = {
                dismissOverlay()
                onComplete()
            }
        )
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showLockInterventionOverlay(unlockCount: Int) {
        if (!canShow(OverlayState.SHOWING_LOCK_INTERVENTION)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_LOCK_INTERVENTION
        val overlay = LockInterventionOverlayView(
            context = context,
            unlockCount = unlockCount,
            onAware = {
                dismissOverlay()
                navigateHome()
            }
        )
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showLaunchLimitOverlay(
        appName: String,
        currentCount: Int,
        limit: Int,
        onOpenAnyway: () -> Unit,
        onSkip: () -> Unit
    ) {
        if (!canShow(OverlayState.SHOWING_LAUNCH_LIMIT)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_LAUNCH_LIMIT
        val overlay = LaunchLimitOverlayView(
            context = context,
            appName = appName,
            currentCount = currentCount,
            limit = limit,
            onOpenAnyway = {
                dismissOverlay()
                onOpenAnyway()
            },
            onSkip = {
                dismissOverlay()
                navigateHome()
                onSkip()
            }
        )
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showScheduleResumeOverlay(currentBand: String, nextChange: String) {
        if (!canShow(OverlayState.SHOWING_SCHEDULE_RESUME)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_SCHEDULE_RESUME
        val overlay = ScheduleResumeOverlayView(
            context = context,
            currentBand = currentBand,
            nextChange = nextChange,
            onDismiss = { dismissOverlay() }
        )
        addOverlayToWindow(overlay)
    }

    @MainThread
    private fun addOverlayToWindow(overlay: View) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try {
            currentOverlay = overlay
            windowManager.addView(overlay, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay", e)
            currentOverlay = null
            currentState = OverlayState.IDLE
        }
    }

    @MainThread
    private fun dismissOverlay() {
        currentOverlay?.let { overlay ->
            try {
                windowManager.removeView(overlay)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove overlay view", e)
            }
            currentOverlay = null
        }
        currentState = OverlayState.IDLE
    }

    @MainThread
    fun navigateHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ContextCompat.startActivity(context, intent, null)
    }

    companion object {
        private const val TAG = "OverlayManager"
    }
}
