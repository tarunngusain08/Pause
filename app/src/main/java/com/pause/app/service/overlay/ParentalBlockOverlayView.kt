package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.view.View
import com.pause.app.R

class ParentalBlockOverlayView(
    context: Context,
    appName: String,
    liftsAt: String,
    emergencyContactName: String?,
    private val onEmergencyContact: () -> Unit,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_parental_block, this, true)
        val appNameText: TextView = findViewById(R.id.parental_block_app_name)
        val liftsAtText: TextView = findViewById(R.id.parental_block_lifts_at)
        val emergencyButton: Button = findViewById(R.id.parental_block_emergency)
        val understandButton: Button = findViewById(R.id.parental_block_understand)

        setBackgroundColor(0xFFF0F4F8.toInt())
        appNameText.text = "$appName is restricted"
        liftsAtText.text = "Lifts at $liftsAt"

        if (emergencyContactName != null) {
            emergencyButton.visibility = View.VISIBLE
            emergencyButton.text = "Call $emergencyContactName"
            emergencyButton.setOnClickListener { onEmergencyContact() }
        } else {
            emergencyButton.visibility = View.GONE
        }

        understandButton.setOnClickListener { onDismiss() }
    }
}
