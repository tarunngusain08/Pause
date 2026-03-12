package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R

class ContentShieldBlockOverlayView(
    context: Context,
    appName: String,
    private val onGoHome: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_content_shield_block, this, true)
        findViewById<TextView>(R.id.overlay_message).text = "$appName is blocked"
        findViewById<Button>(R.id.btn_go_home).setOnClickListener { onGoHome() }
    }
}
