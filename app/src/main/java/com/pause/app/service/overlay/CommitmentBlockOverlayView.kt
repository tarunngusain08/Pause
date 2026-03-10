package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import com.pause.app.R

class CommitmentBlockOverlayView(
    context: Context,
    private val onBreakCommitment: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_commitment_block, this, true)
        findViewById<Button>(R.id.btn_break).setOnClickListener { onBreakCommitment() }
    }
}
