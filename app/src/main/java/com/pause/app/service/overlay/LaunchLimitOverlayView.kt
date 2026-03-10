package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class LaunchLimitOverlayView(
    context: Context,
    appName: String,
    currentCount: Int,
    limit: Int,
    private val onOpenAnyway: () -> Unit,
    private val onSkip: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_launch_limit, this, true)
        val messageText = findViewById<TextView>(R.id.overlay_message)
        val btnOpenAnyway = findViewById<Button>(R.id.btn_open_anyway)
        val btnSkip = findViewById<Button>(R.id.btn_skip)

        messageText.text = context.getString(
            R.string.launch_limit_message,
            appName,
            currentCount,
            limit
        )

        btnOpenAnyway.setOnClickListener { onOpenAnyway() }
        btnSkip.setOnClickListener { onSkip() }
    }
}
