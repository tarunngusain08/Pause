package com.pause.app.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateToStrictSetup: () -> Unit = {},
    onNavigateToContentShield: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val strictSession by viewModel.strictSession.collectAsState(initial = null)
    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var strictRemainingMs by remember { mutableStateOf(0L) }
    var permissionDialogDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(strictSession) {
        if (strictSession == null) {
            strictRemainingMs = 0L
            return@LaunchedEffect
        }
        while (true) {
            val remaining = viewModel.getStrictRemainingMs()
            strictRemainingMs = remaining
            if (remaining <= 0L) break
            delay(1000)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                permissionDialogDismissed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (strictSession != null && strictRemainingMs > 0) {
        val totalDurationMs = (
            (strictSession?.endsAt ?: 0L) - (strictSession?.startedAt ?: 0L)
            ).coerceAtLeast(1L)
        FocusModeActiveScreen(
            remainingMs = strictRemainingMs,
            totalDurationMs = totalDurationMs
        )
        return
    }

    if (!permissionStatus.allGranted && !permissionDialogDismissed) {
        PermissionsDialog(
            overlayMissing = !permissionStatus.overlayGranted,
            accessibilityMissing = !permissionStatus.accessibilityEnabled,
            onDismiss = { permissionDialogDismissed = true },
            onFixOverlay = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onFixAccessibility = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            text = "Focus",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use Focus Mode and Content Shield to stay intentional.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onNavigateToStrictSetup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Focus Mode")
            }

            OutlinedButton(
                onClick = onNavigateToContentShield,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Content Shield")
            }
        }
    }
}

@Composable
private fun FocusModeActiveScreen(
    remainingMs: Long,
    totalDurationMs: Long
) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus-pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focus-scale"
    )
    val minutes = (remainingMs / 1000 / 60).toInt()
    val seconds = ((remainingMs / 1000) % 60).toInt()
    val progress = (1f - (remainingMs.toFloat() / totalDurationMs.toFloat()))
        .coerceIn(0f, 1f)
    val progressPct = (progress * 100).toInt()
    val motivationMessages = listOf(
        "Stay with the plan. Your future self will thank you.",
        "Small moments of focus build strong discipline.",
        "You are training attention, one minute at a time.",
        "Keep going. Discomfort now, clarity later."
    )
    val motivation = motivationMessages[((remainingMs / 15_000L) % motivationMessages.size).toInt()]
    val allowedApps = listOf("Dialer", "Camera", "Clock", "Calculator", "Contacts")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Focus Mode Active",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 10.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.scale(scale)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$progressPct% complete",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = motivation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Allowed apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            allowedApps.forEach { app ->
                Text(
                    text = "- $app",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = "Triple-tap the emergency button on the block screen to exit early",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionsDialog(
    overlayMissing: Boolean,
    accessibilityMissing: Boolean,
    onDismiss: () -> Unit,
    onFixOverlay: () -> Unit,
    onFixAccessibility: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Permissions needed",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Focus needs the following permissions to work correctly:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (overlayMissing) {
                    PermissionRow(
                        icon = Icons.Outlined.Layers,
                        title = "Display over other apps",
                        description = "Required to show Focus Mode and block overlays.",
                        actionLabel = "Open Settings",
                        onAction = onFixOverlay
                    )
                }
                if (accessibilityMissing) {
                    PermissionRow(
                        icon = Icons.Outlined.Accessibility,
                        title = "Accessibility service",
                        description = "Required to detect app changes for Focus Mode and Content Shield.",
                        actionLabel = "Open Settings",
                        onAction = onFixAccessibility
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onAction,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
