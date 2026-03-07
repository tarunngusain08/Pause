package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class ScheduleResumeOverlayView(
    context: Context,
    currentBand: String,
    nextChange: String,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_schedule_resume, this, true)
        val bandText: TextView = findViewById(R.id.schedule_resume_band)
        val nextText: TextView = findViewById(R.id.schedule_resume_next)
        val gotItButton: Button = findViewById(R.id.schedule_resume_got_it)

        setBackgroundColor(0xFFF0F4F8.toInt())
        bandText.text = "Current: $currentBand"
        nextText.text = "Next change: $nextChange"

        gotItButton.setOnClickListener { onDismiss() }
    }
}
