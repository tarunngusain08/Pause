package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.pause.app.R
import com.pause.app.data.db.entity.ReflectionResponse

class ReflectionOverlayView(
    context: Context,
    private val appName: String,
    private val onReasonSelected: (String) -> Unit
) : FrameLayout(context) {

    private val questionText: TextView
    private val appNameText: TextView
    private val btnBored: Button
    private val btnHabit: Button
    private val btnReplying: Button
    private val btnIntentional: Button

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_reflection, this, true)
        questionText = findViewById(R.id.overlay_question)
        appNameText = findViewById(R.id.overlay_app_name)
        btnBored = findViewById(R.id.btn_bored)
        btnHabit = findViewById(R.id.btn_habit)
        btnReplying = findViewById(R.id.btn_replying)
        btnIntentional = findViewById(R.id.btn_intentional)

        appNameText.text = appName

        btnBored.setOnClickListener { onReasonSelected(ReflectionResponse.REASON_BORED) }
        btnHabit.setOnClickListener { onReasonSelected(ReflectionResponse.REASON_HABIT) }
        btnReplying.setOnClickListener { onReasonSelected(ReflectionResponse.REASON_REPLYING) }
        btnIntentional.setOnClickListener { onReasonSelected(ReflectionResponse.REASON_INTENTIONAL) }
    }
}
