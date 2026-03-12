package com.pause.app.ui.strict

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pause.app.ui.common.rememberAppIcon
import com.pause.app.ui.home.resolveInstalledAllowedApps

private const val MAX_DURATION_MS = 8 * 60 * 60 * 1000L

@Composable
fun StrictModeSetupScreen(
    onSessionStarted: () -> Unit,
    onBack: () -> Unit,
    viewModel: StrictModeSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    LaunchedEffect(activeSession) {
        if (activeSession != null) onSessionStarted()
    }

    val totalMin = (uiState.selectedDurationMs / 60_000).toInt()
    val hours = totalMin / 60
    val minutes = totalMin % 60
    val durationLabel = when {
        hours == 0 -> "$minutes min"
        minutes == 0 -> if (hours == 1) "1 hour" else "$hours hours"
        else -> "${hours}h ${minutes}m"
    }
    val durationProgress = (uiState.selectedDurationMs.toFloat() / MAX_DURATION_MS.toFloat())
        .coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Focus Mode",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Lock distractions for a fixed period. Only essential apps stay accessible.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = { durationProgress },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 7.dp
                )
                Column {
                    Text(
                        text = "Selected duration",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = durationLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Max 8 hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Text(
            text = "Presets",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        StrictModeSetupViewModel.DURATION_PRESETS.chunked(2).forEachIndexed { rowIndex, rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPresets.forEachIndexed { colIndex, (_, label) ->
                    val presetIndex = rowIndex * 2 + colIndex
                    val selected = !uiState.useCustomDuration && uiState.selectedPresetIndex == presetIndex
                    if (selected) {
                        Button(
                            onClick = { viewModel.selectPreset(presetIndex) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.selectPreset(presetIndex) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }

        Text(
            text = "Custom duration",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.customMinutesText,
                onValueChange = { new ->
                    viewModel.setCustomMinutesText(new)
                    viewModel.selectCustomDuration()
                },
                modifier = Modifier.width(120.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Text(
                text = "minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            val context = LocalContext.current
            val allowedApps = remember {
                resolveInstalledAllowedApps(context.packageManager)
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Allowed apps during Focus Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                allowedApps.forEach { (pkg, appName) ->
                    SetupAllowedAppRow(packageName = pkg, appName = appName)
                }
            }
        }

        uiState.startError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        val durationValid = uiState.selectedDurationMs >= 1 * 60_000L
        Button(
            onClick = { viewModel.showFirstConfirmation() },
            enabled = !uiState.isStarting && durationValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isStarting) "Starting..." else "Start Focus Mode")
        }
    }

    if (uiState.showFirstConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFirstConfirmation() },
            title = { Text("Once started, no Open Anyway button") },
            text = {
                Text(
                    "You will not be able to open blocked apps until the session ends. " +
                        "Emergency exit requires triple-tap and confirmation."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmFirstAndShowSecond() }) {
                    Text("I understand")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissFirstConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showSecondConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSecondConfirmation() },
            title = { Text("Are you absolutely sure?") },
            text = { Text("There is no way to bypass this session except the emergency exit procedure.") },
            confirmButton = {
                Button(onClick = { viewModel.startSession() }) {
                    Text("Start Session")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissSecondConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SetupAllowedAppRow(packageName: String, appName: String) {
    val iconBitmap = rememberAppIcon(packageName)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(appName, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
