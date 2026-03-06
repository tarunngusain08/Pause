package com.pause.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SoftBlue = Color(0xFF4A90A4)
private val SoftBlueDark = Color(0xFF2E5F6B)
private val CalmBackground = Color(0xFFF0F4F8)
private val CalmSurface = Color(0xFFE8F4F7)
private val OnPrimary = Color.White
private val OnSurface = Color(0xFF1A1A2E)
private val OnSurfaceVariant = Color(0xFF5A5A6E)

private val LightColorScheme = lightColorScheme(
    primary = SoftBlue,
    onPrimary = OnPrimary,
    primaryContainer = CalmSurface,
    onPrimaryContainer = OnSurface,
    surface = CalmBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Color(0xFFB0BEC5)
)

private val DarkColorScheme = darkColorScheme(
    primary = SoftBlue,
    onPrimary = OnPrimary,
    primaryContainer = SoftBlueDark,
    onPrimaryContainer = Color.White,
    surface = Color(0xFF1A1A2E),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF546E7A)
)

@Composable
fun PauseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
