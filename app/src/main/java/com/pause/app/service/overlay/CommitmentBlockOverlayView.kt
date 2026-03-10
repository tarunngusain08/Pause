package com.pause.app.service.overlay

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.pause.app.R

class CommitmentBlockOverlayView(
    context: Context,
    private val onBreakCommitment: () -> Unit
) : FrameLayout(context) {

    private var holdAnimator: ObjectAnimator? = null
    private val progressBar: ProgressBar

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_commitment_block, this, true)
        progressBar = findViewById(R.id.progress_break_hold)
        val btnBreak = findViewById<Button>(R.id.btn_break)

        btnBreak.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                    holdAnimator = ObjectAnimator.ofInt(
                        progressBar, "progress", 0, 100
                    ).apply {
                        duration = HOLD_DURATION_MS
                        addUpdateListener { anim ->
                            if ((anim.animatedValue as Int) >= 100) {
                                triggerBreak(view)
                            }
                        }
                        start()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdAnimator?.cancel()
                    holdAnimator = null
                    progressBar.visibility = View.GONE
                    progressBar.progress = 0
                    true
                }
                else -> false
            }
        }
    }

    private fun triggerBreak(view: View) {
        holdAnimator?.cancel()
        holdAnimator = null
        progressBar.visibility = View.GONE
        vibrate(view.context)
        onBreakCommitment()
    }

    private fun vibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        }
    }

    companion object {
        private const val HOLD_DURATION_MS = 2000L
    }
}
