package com.pause.app.service.overlay

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.view.View
import com.pause.app.R

class PINEntryOverlayView(
    context: Context,
    private val onSuccess: (String) -> Unit,
    private val onForgotPIN: () -> Unit
) : FrameLayout(context) {

    private val pinInput: EditText
    private val errorText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.overlay_pin_entry, this, true)
        pinInput = findViewById(R.id.pin_entry_input)
        errorText = findViewById(R.id.pin_entry_error)
        val submitButton: Button = findViewById(R.id.pin_entry_submit)
        val forgotButton: TextView = findViewById(R.id.pin_entry_forgot)

        setBackgroundColor(0xFFF0F4F8.toInt())

        submitButton.setOnClickListener {
            val pin = pinInput.text.toString()
            if (pin.length == 6) {
                onSuccess(pin)
            } else {
                errorText.visibility = View.VISIBLE
                errorText.text = "Enter 6 digits"
            }
        }

        forgotButton.setOnClickListener { onForgotPIN() }
    }

    fun showError(message: String) {
        errorText.visibility = View.VISIBLE
        errorText.text = message
    }

    fun clearError() {
        errorText.visibility = View.GONE
    }
}
