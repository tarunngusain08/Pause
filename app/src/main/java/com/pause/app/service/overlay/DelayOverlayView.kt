package com.pause.app.service.overlay

import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class DelayOverlayView(context: Context) : FrameLayout(context) {

    private val appNameLabel: TextView
    private val countdownText: TextView
    private val cancelButton: Button

    private var countDownTimer: CountDownTimer? = null
    private var onCancelListener: (() -> Unit)? = null
    private var onCompleteListener: (() -> Unit)? = null
    private var isDismissed = false

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_delay, this, true)
        appNameLabel = findViewById(R.id.overlay_app_name)
        countdownText = findViewById(R.id.overlay_countdown)
        cancelButton = findViewById(R.id.overlay_cancel_button)

        setBackgroundColor(0xFFF0F4F8.toInt())
        cancelButton.setOnClickListener {
            if (isDismissed) return@setOnClickListener
            isDismissed = true
            cancelButton.isEnabled = false
            countDownTimer?.cancel()
            onCancelListener?.invoke()
        }
    }

    fun show(appName: String, delaySeconds: Int) {
        appNameLabel.text = "Opening $appName in..."
        countdownText.text = delaySeconds.toString()

        countDownTimer = object : CountDownTimer(delaySeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                countdownText.text = secondsLeft.toString()
            }

            override fun onFinish() {
                if (isDismissed) return
                isDismissed = true
                onCompleteListener?.invoke()
            }
        }.start()
    }

    fun setOnCancelListener(listener: () -> Unit) {
        onCancelListener = listener
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onCompleteListener = listener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        countDownTimer?.cancel()
    }
}
