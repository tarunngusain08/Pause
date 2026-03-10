package com.pause.app.ui.strict

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StrictModeSetupScreen(
    onSessionStarted: () -> Unit,
    onBack: () -> Unit,
    viewModel: StrictModeSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            onSessionStarted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Strict Mode",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No escape hatch. Blocked apps stay blocked until the session ends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Duration",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Selected duration summary chip
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val totalMin = (uiState.selectedDurationMs / 60_000).toInt()
            val h = totalMin / 60
            val m = totalMin % 60
            val label = when {
                h == 0 -> "$m min"
                m == 0 -> if (h == 1) "1 hour" else "$h hours"
                else -> "${h}h ${m}m"
            }
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Quick preset chips (+ Custom)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StrictModeSetupViewModel.DURATION_PRESETS.forEachIndexed { index, (_, label) ->
                val selected = !uiState.useCustomDuration && uiState.selectedPresetIndex == index
                if (selected) {
                    Button(
                        onClick = { viewModel.selectPreset(index) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.selectPreset(index) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                }
            }
            if (uiState.useCustomDuration) {
                Button(
                    onClick = { viewModel.selectCustomDuration() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Custom", style = MaterialTheme.typography.labelMedium) }
            } else {
                OutlinedButton(
                    onClick = { viewModel.selectCustomDuration() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Custom", style = MaterialTheme.typography.labelMedium) }
            }
        }

        // Custom hour/minute steppers — visible only when Custom is selected
        if (uiState.useCustomDuration) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeUnitStepper(
                        label = "Hours",
                        value = uiState.customHours,
                        onDecrement = { viewModel.setCustomHours(uiState.customHours - 1) },
                        onIncrement = { viewModel.setCustomHours(uiState.customHours + 1) },
                        canDecrement = uiState.customHours > 0 || uiState.customMinutes > 0,
                        canIncrement = uiState.customHours < 8
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TimeUnitStepper(
                        label = "Minutes",
                        value = uiState.customMinutes,
                        onDecrement = { viewModel.setCustomMinutes(uiState.customMinutes - 5) },
                        onIncrement = { viewModel.setCustomMinutes(uiState.customMinutes + 5) },
                        canDecrement = uiState.customMinutes >= 5 || uiState.customHours > 0,
                        canIncrement = uiState.customMinutes < 55
                    )
                }
                val totalValid = uiState.selectedDurationMs >= 5 * 60_000L
                if (!totalValid) {
                    Text(
                        text = "Minimum duration is 5 minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Block these apps",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.selectAllApps() }) {
            Text("Select All")
        }
        Spacer(modifier = Modifier.height(8.dp))
        uiState.monitoredApps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = app.packageName in uiState.selectedPackages,
                    onCheckedChange = { viewModel.toggleAppSelection(app.packageName) }
                )
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        uiState.startError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        val durationValid = uiState.selectedDurationMs >= 5 * 60_000L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Button(
                onClick = { viewModel.showFirstConfirmation() },
                enabled = uiState.selectedPackages.isNotEmpty() && !uiState.isStarting && durationValid
            ) {
                Text(if (uiState.isStarting) "Starting..." else "Start Strict Mode")
            }
        }
    }

    if (uiState.showFirstConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFirstConfirmation() },
            title = { Text("Once started, no Open Anyway button") },
            text = { Text("You will not be able to open blocked apps until the session ends. Emergency exit requires triple-tap and confirmation.") },
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
private fun TimeUnitStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    canIncrement: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrement,
                enabled = canDecrement,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease $label",
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "%02d".format(value),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(min = 52.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = onIncrement,
                enabled = canIncrement,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase $label",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
