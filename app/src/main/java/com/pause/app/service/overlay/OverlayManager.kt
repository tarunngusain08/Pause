package com.pause.app.service.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import com.pause.app.service.strict.EmergencyExitController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val windowManager: WindowManager,
    private val emergencyExitController: EmergencyExitController
) {
    private var currentOverlay: View? = null
    private var currentState: OverlayState = OverlayState.IDLE

    @MainThread
    fun getState(): OverlayState = currentState

    @MainThread
    private fun hasOverlayPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    @MainThread
    private fun canShow(newState: OverlayState): Boolean {
        if (newState.isInformational) return currentState == OverlayState.IDLE
        if (newState.priority > currentState.priority) {
            dismissOverlay()
            return true
        }
        return currentState == OverlayState.IDLE
    }

    @MainThread
    fun dismiss() {
        val dismissable = setOf(
            OverlayState.SHOWING_PARENTAL_BLOCK,
            OverlayState.SHOWING_POWER_BLOCK,
            OverlayState.SHOWING_EMERGENCY_CONFIRM,
            OverlayState.SHOWING_LOCK_INTERVENTION,
            OverlayState.SHOWING_PIN_ENTRY
        )
        if (currentState in dismissable) {
            dismissOverlay()
        }
    }

    @MainThread
    fun dismissBlockIfAllowed() {
        if (currentState == OverlayState.SHOWING_STRICT_BLOCK ||
            currentState == OverlayState.SHOWING_CONTENT_SHIELD_BLOCK
        ) {
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
            currentState != OverlayState.SHOWING_STRICT_BLOCK
        ) return
        if (!hasOverlayPermission()) return

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
        navigateHome()
    }

    @MainThread
    fun showPowerMenuBlockOverlay(remainingMs: Long) {
        if (!canShow(OverlayState.SHOWING_POWER_BLOCK)) return
        if (!hasOverlayPermission()) return

        currentState = OverlayState.SHOWING_POWER_BLOCK
        val overlay = PowerMenuBlockOverlayView(context, remainingMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showSessionResumeOverlay(remainingMs: Long) {
        if (!canShow(OverlayState.SHOWING_SESSION_RESUME)) return
        if (!hasOverlayPermission()) return

        currentState = OverlayState.SHOWING_SESSION_RESUME
        val overlay = SessionResumeOverlayView(context, remainingMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showSessionCompleteOverlay(durationMs: Long) {
        if (!canShow(OverlayState.SHOWING_SESSION_COMPLETE)) return
        if (!hasOverlayPermission()) return

        currentState = OverlayState.SHOWING_SESSION_COMPLETE
        val overlay = SessionCompleteOverlayView(context, durationMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    @MainThread
    fun showEmergencyConfirmOverlay(onConfirm: () -> Unit, onCancel: () -> Unit) {
        if (!canShow(OverlayState.SHOWING_EMERGENCY_CONFIRM)) return
        if (!hasOverlayPermission()) return

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
            currentState != OverlayState.SHOWING_PARENTAL_BLOCK
        ) return
        if (!hasOverlayPermission()) return

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
        if (!hasOverlayPermission()) return

        currentState = OverlayState.SHOWING_PIN_ENTRY
        var pinOverlayRef: PINEntryOverlayView? = null
        val overlay = PINEntryOverlayView(
            context = context,
            onSuccess = { pin ->
                if (onPinAttempt != null) {
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
    fun showContentShieldBlockOverlay(appName: String) {
        if (!canShow(OverlayState.SHOWING_CONTENT_SHIELD_BLOCK) &&
            currentState != OverlayState.SHOWING_CONTENT_SHIELD_BLOCK
        ) return
        if (!hasOverlayPermission()) return

        dismissOverlay()
        currentState = OverlayState.SHOWING_CONTENT_SHIELD_BLOCK
        val overlay = ContentShieldBlockOverlayView(
            context = context,
            appName = appName,
            onGoHome = {
                dismissOverlay()
                navigateHome()
            }
        )
        addOverlayToWindow(overlay)
        navigateHome()
    }

    @MainThread
    fun showLockInterventionOverlay(unlockCount: Int) {
        if (!canShow(OverlayState.SHOWING_LOCK_INTERVENTION)) return
        if (!hasOverlayPermission()) return

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
    fun showScheduleResumeOverlay(currentBand: String, nextChange: String) {
        if (!canShow(OverlayState.SHOWING_SCHEDULE_RESUME)) return
        if (!hasOverlayPermission()) return

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
        val isBlockOverlay = currentState in BLOCK_OVERLAY_STATES
        val isStrictBlock = currentState == OverlayState.SHOWING_STRICT_BLOCK
        val needsFocus = currentState == OverlayState.SHOWING_PIN_ENTRY
        var flags = when {
            needsFocus -> {
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            }
            isStrictBlock -> {
                // Fully modal: capture all touch events so nothing behind can be tapped.
                // FLAG_SHOW_WHEN_LOCKED / FLAG_TURN_SCREEN_ON are deprecated API 27+ but the
                // Window-level alternatives only apply to Activities, not system overlays.
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SECURE or
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            }
            else -> {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            }
        }
        if (isBlockOverlay && !isStrictBlock) {
            flags = flags or WindowManager.LayoutParams.FLAG_SECURE
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
            flags,
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
            (overlay as? TimerCancellable)?.cancelTimers()
            try {
                windowManager.removeView(overlay)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove overlay view", e)
                overlay.visibility = View.GONE
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

        private val BLOCK_OVERLAY_STATES = setOf(
            OverlayState.SHOWING_STRICT_BLOCK,
            OverlayState.SHOWING_CONTENT_SHIELD_BLOCK,
            OverlayState.SHOWING_PARENTAL_BLOCK,
            OverlayState.SHOWING_PIN_ENTRY
        )
    }
}
