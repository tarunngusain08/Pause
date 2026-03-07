package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class SessionResumeOverlayView(
    context: Context,
    remainingMs: Long,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_session_resume, this, true)
        val remainingText: TextView = findViewById(R.id.session_resume_remaining)
        val gotItButton: Button = findViewById(R.id.session_resume_got_it)

        setBackgroundColor(0xFFF0F4F8.toInt())
        val totalSeconds = (remainingMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        remainingText.text = String.format("%d:%02d remaining", minutes, seconds)

        gotItButton.setOnClickListener { onDismiss() }
    }
}
