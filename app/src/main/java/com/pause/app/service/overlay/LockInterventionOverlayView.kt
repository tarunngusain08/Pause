package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class LockInterventionOverlayView(
    context: Context,
    unlockCount: Int,
    private val onAware: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_lock_intervention, this, true)
        val messageText = findViewById<TextView>(R.id.overlay_message)
        val btnAware = findViewById<Button>(R.id.btn_aware)

        messageText.text = context.getString(
            R.string.lock_intervention_message,
            unlockCount
        )

        btnAware.setOnClickListener { onAware() }
    }
}
