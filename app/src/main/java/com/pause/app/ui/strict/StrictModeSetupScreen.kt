package com.pause.app.ui.strict

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        Text(
            text = "Focus Mode",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StrictModeSetupViewModel.DURATION_PRESETS.forEachIndexed { index, (_, label) ->
                val selected = !uiState.useCustomDuration && uiState.selectedPresetIndex == index
                if (selected) {
                    Button(
                        onClick = { viewModel.selectPreset(index) },
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.selectPreset(index) },
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                }
            }
            if (uiState.useCustomDuration) {
                Button(
                    onClick = { viewModel.selectCustomDuration() },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Custom", style = MaterialTheme.typography.labelMedium) }
            } else {
                OutlinedButton(
                    onClick = { viewModel.selectCustomDuration() },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Custom", style = MaterialTheme.typography.labelMedium) }
            }
        }

        if (uiState.useCustomDuration) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.customMinutesRaw.toString(),
                        onValueChange = { new ->
                            val parsed = new.filter { it.isDigit() }.toIntOrNull() ?: 0
                            viewModel.setCustomMinutesRaw(parsed)
                        },
                        label = { Text("Custom minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    val rounded = uiState.roundedCustomMinutes
                    val raw = uiState.customMinutesRaw
                    if (raw != rounded && raw in 0..480) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Will be rounded to $rounded minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Allowed apps during Focus Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("Dialer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Camera", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Clock", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Calculator", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Contacts", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        uiState.startError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        val durationValid = uiState.selectedDurationMs >= 5 * 60_000L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = { viewModel.showFirstConfirmation() },
                enabled = !uiState.isStarting && durationValid,
                modifier = Modifier.weight(2f)
            ) {
                Text(if (uiState.isStarting) "Starting..." else "Start Focus Mode")
            }
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
