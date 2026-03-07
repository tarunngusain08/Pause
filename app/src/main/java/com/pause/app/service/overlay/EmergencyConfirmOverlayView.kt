package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import com.pause.app.R

class EmergencyConfirmOverlayView(
    context: Context,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_emergency_confirm, this, true)
        val cancelButton: Button = findViewById(R.id.emergency_confirm_cancel)
        val endButton: Button = findViewById(R.id.emergency_confirm_end)

        setBackgroundColor(0xFFF0F4F8.toInt())
        cancelButton.setOnClickListener { onCancel() }
        endButton.setOnClickListener { onConfirm() }
    }
}
