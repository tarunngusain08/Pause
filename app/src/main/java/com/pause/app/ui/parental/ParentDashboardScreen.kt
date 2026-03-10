package com.pause.app.ui.parental

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ParentDashboardScreen(
    onBack: () -> Unit,
    onNavigateToWebFilter: () -> Unit = {},
    viewModel: ParentDashboardViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Parent Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Configure schedule, blocked apps, and settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onNavigateToWebFilter,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Web Filter")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}
