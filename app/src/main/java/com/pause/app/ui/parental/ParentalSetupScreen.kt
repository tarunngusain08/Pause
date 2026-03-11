package com.pause.app.ui.parental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val STEP_TITLES = listOf(
    "Set PIN",
    "Recovery phrase",
    "Schedule",
    "App restrictions",
    "Emergency contact"
)

private val STEP_DESCRIPTIONS = listOf(
    "Create a secure PIN that your child cannot guess.",
    "Write down your recovery phrase and store it somewhere safe.",
    "Define allowed screen-time windows for each day.",
    "Choose which apps are blocked or require your approval.",
    "Add an emergency contact your child can reach when Pause is active."
)

@Composable
fun ParentalSetupScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: ParentalSetupViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val totalSteps = viewModel.totalSteps
    val progress = (currentStep + 1).toFloat() / totalSteps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Parental Control Setup",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Step indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = STEP_TITLES.getOrElse(currentStep) { "" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = STEP_TITLES.getOrElse(currentStep) { "Setup" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = STEP_DESCRIPTIONS.getOrElse(currentStep) { "" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (currentStep == 0) onBack() else viewModel.advanceStep()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (currentStep == 0) "Back" else "Skip step")
            }
            if (currentStep < totalSteps - 1) {
                Button(
                    onClick = { viewModel.advanceStep() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = { viewModel.completeSetup(onComplete) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Finish Setup")
                }
            }
        }
    }
}
