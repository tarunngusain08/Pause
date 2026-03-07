package com.pause.app.ui.strict

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StrictModeSetupScreen(
    onSessionStarted: () -> Unit,
    onBack: () -> Unit,
    viewModel: StrictModeSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    if (activeSession != null) {
        onSessionStarted()
        return
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StrictModeSetupViewModel.DURATION_PRESETS.forEach { (ms, label) ->
                val selected = uiState.selectedDurationMs == ms
                if (selected) {
                    Button(
                        onClick = { viewModel.setDuration(ms) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.setDuration(ms) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Button(
                onClick = { viewModel.showFirstConfirmation() },
                enabled = uiState.selectedPackages.isNotEmpty() && !uiState.isStarting
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
