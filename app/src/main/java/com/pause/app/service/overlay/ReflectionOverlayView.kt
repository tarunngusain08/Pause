package com.pause.app.service.overlay

import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.pause.app.R
import com.pause.app.data.db.entity.ReflectionResponse

class ReflectionOverlayView(
    context: Context,
    private val appName: String,
    private val onReasonSelected: (String) -> Unit
) : FrameLayout(context) {

    private val appNameText: TextView
    private val btnBored: Button
    private val btnHabit: Button
    private val btnReplying: Button
    private val btnIntentional: Button
    private val progressTimeout: ProgressBar

    private var isDismissed = false
    private val timeoutTimer = object : CountDownTimer(TIMEOUT_MS, TICK_INTERVAL_MS) {
        override fun onTick(millisUntilFinished: Long) {
            val progress = (millisUntilFinished * 100 / TIMEOUT_MS).toInt()
            progressTimeout.progress = progress
        }
        override fun onFinish() {
            if (!isDismissed) {
                isDismissed = true
                onReasonSelected(ReflectionResponse.REASON_TIMED_OUT)
            }
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_reflection, this, true)
        appNameText = findViewById(R.id.overlay_app_name)
        btnBored = findViewById(R.id.btn_bored)
        btnHabit = findViewById(R.id.btn_habit)
        btnReplying = findViewById(R.id.btn_replying)
        btnIntentional = findViewById(R.id.btn_intentional)
        progressTimeout = findViewById(R.id.progress_timeout)

        appNameText.text = appName

        val reasonClickListener = { reason: String ->
            if (!isDismissed) {
                isDismissed = true
                timeoutTimer.cancel()
                onReasonSelected(reason)
            }
        }
        btnBored.setOnClickListener { reasonClickListener(ReflectionResponse.REASON_BORED) }
        btnHabit.setOnClickListener { reasonClickListener(ReflectionResponse.REASON_HABIT) }
        btnReplying.setOnClickListener { reasonClickListener(ReflectionResponse.REASON_REPLYING) }
        btnIntentional.setOnClickListener { reasonClickListener(ReflectionResponse.REASON_INTENTIONAL) }

        timeoutTimer.start()
    }

    override fun onDetachedFromWindow() {
        isDismissed = true
        timeoutTimer.cancel()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val TIMEOUT_MS = 30_000L
        private const val TICK_INTERVAL_MS = 300L
    }
}
