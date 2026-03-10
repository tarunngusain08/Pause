package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import com.pause.app.R

class AllowanceReachedOverlayView(
    context: Context,
    private val onOpenAnyway: () -> Unit,
    private val onImDone: () -> Unit
) : FrameLayout(context) {

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_allowance_reached, this, true)
        findViewById<Button>(R.id.btn_open_anyway).setOnClickListener { onOpenAnyway() }
        findViewById<Button>(R.id.btn_im_done).setOnClickListener { onImDone() }
    }
}
