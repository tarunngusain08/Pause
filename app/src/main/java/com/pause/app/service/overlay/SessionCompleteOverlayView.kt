package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class SessionCompleteOverlayView(
    context: Context,
    durationMs: Long,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_session_complete, this, true)
        val durationText: TextView = findViewById(R.id.session_complete_duration)
        val dismissButton: Button = findViewById(R.id.session_complete_dismiss)

        setBackgroundColor(0xFF1A1A2E.toInt())
        val hours = (durationMs / (1000 * 60 * 60)).toInt()
        val minutes = ((durationMs / (1000 * 60)) % 60).toInt()
        durationText.text = when {
            hours > 0 -> "You stayed focused for $hours hour${if (hours > 1) "s" else ""}. Well done."
            else -> "You stayed focused for $minutes minute${if (minutes != 1) "s" else ""}. Well done."
        }

        dismissButton.setOnClickListener { onDismiss() }
    }
}
