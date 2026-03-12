package com.pause.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pause.app.ui.navigation.PauseNavGraph
import com.pause.app.ui.theme.PauseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PauseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PauseNavGraph()
                }
            }
        }
        // Post to the decor view so the window/display are fully attached before we query modes.
        window.decorView.post { requestHighRefreshRate() }
    }

    /**
     * Requests the highest refresh rate the display supports (e.g. 120 Hz).
     * Must be called after the window is attached (e.g. from decorView.post).
     */
    @Suppress("DEPRECATION")
    private fun requestHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bestMode = display?.supportedModes
                ?.maxByOrNull { it.refreshRate }
            if (bestMode != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = bestMode.modeId
                    preferMinimalPostProcessing = true
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bestMode = windowManager.defaultDisplay.supportedModes
                .maxByOrNull { it.refreshRate }
            if (bestMode != null) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = bestMode.modeId
                }
            }
        }
    }
}
