package com.pause.app.service.overlay

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R
import com.pause.app.service.strict.EmergencyExitController
import com.pause.app.service.strict.EmergencyTapResult

class StrictBlockOverlayView(
    context: Context,
    private val appName: String,
    private val remainingMs: Long,
    private val emergencyExitController: EmergencyExitController,
    private val onGoBack: () -> Unit,
    private val onShowConfirmation: () -> Unit
) : FrameLayout(context), TimerCancellable {

    private val appNameLabel: TextView
    private val remainingText: TextView
    private val dotsText: TextView
    private val goBackButton: Button
    private val emergencyButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable {
        emergencyExitController.reset()
        updateDots()
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_strict_block, this, true)
        appNameLabel = findViewById(R.id.strict_block_app_name)
        remainingText = findViewById(R.id.strict_block_remaining)
        dotsText = findViewById(R.id.strict_block_dots)
        goBackButton = findViewById(R.id.strict_block_go_back)
        emergencyButton = findViewById(R.id.strict_block_emergency_button)

        appNameLabel.text = "$appName is blocked"
        updateRemaining(remainingMs)

        goBackButton.setOnClickListener { onGoBack() }

        emergencyButton.setOnClickListener {
            handler.removeCallbacks(resetRunnable)
            when (emergencyExitController.onEmergencyTapped()) {
                is EmergencyTapResult.TapRegistered -> {
                    updateDots()
                    handler.postDelayed(resetRunnable, EmergencyExitController.TAP_WINDOW_MS)
                }
                is EmergencyTapResult.ShowConfirmation -> {
                    onShowConfirmation()
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation(AnimationUtils.loadAnimation(context, R.anim.overlay_fade_in))
    }

    override fun onDetachedFromWindow() {
        cancelTimers()
        super.onDetachedFromWindow()
    }

    override fun cancelTimers() {
        handler.removeCallbacks(resetRunnable)
    }

    fun updateRemaining(ms: Long) {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        remainingText.text = String.format("%d:%02d remaining", minutes, seconds)
    }

    private fun updateDots() {
        val tapCount = emergencyExitController.getTapCount()
        dotsText.text = buildString {
            repeat(EmergencyExitController.REQUIRED_TAPS) {
                append(if (it < tapCount) "● " else "○ ")
            }
        }.trim()
    }
}
