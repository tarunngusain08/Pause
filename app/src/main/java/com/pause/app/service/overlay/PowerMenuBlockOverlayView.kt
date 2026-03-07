package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class PowerMenuBlockOverlayView(
    context: Context,
    remainingMs: Long,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_power_block, this, true)
        val remainingText: TextView = findViewById(R.id.power_block_remaining)
        val understandButton: Button = findViewById(R.id.power_block_understand)

        setBackgroundColor(0xFFF0F4F8.toInt())
        val totalSeconds = (remainingMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        remainingText.text = String.format("%d:%02d remaining", minutes, seconds)

        understandButton.setOnClickListener { onDismiss() }
    }
}
