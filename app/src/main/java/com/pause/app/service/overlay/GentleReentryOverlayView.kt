package com.pause.app.service.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A 1.5-second fade-in/fade-out overlay shown on device unlock when the unlock count
 * is below the intervention threshold. Displays a brief mindful pause with the current time.
 */
class GentleReentryOverlayView(
    context: Context,
    private val onDismiss: () -> Unit
) : FrameLayout(context), TimerCancellable {

    private val handler = Handler(Looper.getMainLooper())
    private var dismissed = false
    private var animatorSet: AnimatorSet? = null

    init {
        setBackgroundColor(0xCC1A1A2E.toInt())
        alpha = 0f

        val timeLabel = TextView(context).apply {
            text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            textSize = 36f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        val pauseLabel = TextView(context).apply {
            text = "Pause. Breathe."
            textSize = 16f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(timeLabel)
            addView(pauseLabel)
        }

        addView(
            container,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        )

        // Fade in over 400ms, hold 700ms, fade out over 400ms = 1500ms total
        val fadeIn = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply { duration = 400 }
        val fadeOut = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
            duration = 400
            startDelay = 1100
        }
        animatorSet = AnimatorSet().apply {
            playTogether(fadeIn, fadeOut)
            start()
        }

        handler.postDelayed({
            if (!dismissed) {
                dismissed = true
                onDismiss()
            }
        }, 1500)
    }

    override fun cancelTimers() {
        dismissed = true
        handler.removeCallbacksAndMessages(null)
        animatorSet?.cancel()
    }

    override fun onDetachedFromWindow() {
        cancelTimers()
        super.onDetachedFromWindow()
    }
}
