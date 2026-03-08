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

    fun getState(): OverlayState = currentState

    fun showDelayOverlay(packageName: String, appName: String, delaySeconds: Int) {
        if (currentState != OverlayState.IDLE) return

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
                            delayDurationSeconds = delaySeconds
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
                            delayDurationSeconds = delaySeconds
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
    fun dismiss() {
        val dismissable = setOf(
            OverlayState.SHOWING_STRICT_BLOCK,
            OverlayState.SHOWING_DELAY,
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

    fun showStrictBlockOverlay(
        appName: String,
        remainingMs: Long,
        onEmergencyConfirm: () -> Unit
    ) {
        if (currentState != OverlayState.IDLE && currentState != OverlayState.SHOWING_STRICT_BLOCK) return
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

    fun showPowerMenuBlockOverlay(remainingMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        dismissOverlay()
        currentState = OverlayState.SHOWING_POWER_BLOCK
        val overlay = PowerMenuBlockOverlayView(context, remainingMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    fun showSessionResumeOverlay(remainingMs: Long) {
        if (currentState != OverlayState.IDLE) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        currentState = OverlayState.SHOWING_SESSION_RESUME
        val overlay = SessionResumeOverlayView(context, remainingMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    fun showSessionCompleteOverlay(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        dismissOverlay()
        currentState = OverlayState.SHOWING_SESSION_COMPLETE
        val overlay = SessionCompleteOverlayView(context, durationMs) { dismissOverlay() }
        addOverlayToWindow(overlay)
    }

    fun showEmergencyConfirmOverlay(onConfirm: () -> Unit, onCancel: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        dismissOverlay()
        currentState = OverlayState.SHOWING_EMERGENCY_CONFIRM
        val overlay = EmergencyConfirmOverlayView(context, onConfirm, onCancel)
        addOverlayToWindow(overlay)
    }

    fun showParentalBlockOverlay(
        appName: String,
        liftsAt: String,
        emergencyContact: String?,
        onEmergencyContact: () -> Unit = {},
        onDismiss: () -> Unit = { dismissOverlay() }
    ) {
        if (currentState != OverlayState.IDLE && currentState != OverlayState.SHOWING_PARENTAL_BLOCK) return
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

    fun showPINEntryOverlay(onSuccess: (String) -> Unit, onForgotPIN: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        dismissOverlay()
        currentState = OverlayState.SHOWING_PIN_ENTRY
        val overlay = PINEntryOverlayView(
            context = context,
            onSuccess = { pin -> dismissOverlay(); onSuccess(pin) },
            onForgotPIN = { dismissOverlay(); onForgotPIN() }
        )
        addOverlayToWindow(overlay)
    }

    fun showScheduleResumeOverlay(currentBand: String, nextChange: String) {
        if (currentState != OverlayState.IDLE) return
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

    private fun dismissOverlay() {
        currentOverlay?.let { overlay ->
            try {
                windowManager.removeView(overlay)
            } catch (_: Exception) { }
            currentOverlay = null
        }
        currentState = OverlayState.IDLE
    }

    private fun navigateHome() {
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
