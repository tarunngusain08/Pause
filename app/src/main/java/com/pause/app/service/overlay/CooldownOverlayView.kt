package com.pause.app.service.overlay

import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class CooldownOverlayView(
    context: Context,
    durationSeconds: Int,
    private val onCancel: () -> Unit,
    private val onComplete: () -> Unit
) : FrameLayout(context), TimerCancellable {

    private val countdownText: TextView
    private var timer: CountDownTimer? = null
    private var isDismissed = false

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_cooldown, this, true)
        countdownText = findViewById(R.id.overlay_countdown)
        findViewById<Button>(R.id.btn_cancel).setOnClickListener { onCancel() }

        timer = object : CountDownTimer(durationSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownText.text = (millisUntilFinished / 1000).toInt().toString()
            }

            override fun onFinish() {
                if (!isDismissed) onComplete()
            }
        }.start()
    }

    override fun cancelTimers() {
        isDismissed = true
        timer?.cancel()
    }

    override fun onDetachedFromWindow() {
        cancelTimers()
        super.onDetachedFromWindow()
    }
}
