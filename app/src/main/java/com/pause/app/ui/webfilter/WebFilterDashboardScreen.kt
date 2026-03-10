package com.pause.app.ui.webfilter

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.VpnService
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pause.app.service.webfilter.PauseVpnService

@Composable
fun WebFilterDashboardScreen(
    onNavigateToBlacklist: () -> Unit,
    onNavigateToKeywords: () -> Unit,
    onNavigateToUrlLog: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onBack: () -> Unit,
    viewModel: WebFilterDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Web Filter",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Filter websites by domain and keywords.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VPN filtering",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = uiState.vpnEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                // VPN permission not yet granted; send user to system dialog.
                                // Do NOT call setVpnEnabled(true) yet - only do so after the
                                // VPN service actually starts (handled via ON_RESUME refresh).
                                context.startActivity(intent)
                            } else {
                                // Permission already granted; start the service and persist.
                                viewModel.setVpnEnabled(true)
                                val svcIntent = Intent(context, PauseVpnService::class.java)
                                    .setAction(PauseVpnService.ACTION_START)
                                context.startForegroundService(svcIntent)
                            }
                        } else {
                            viewModel.setVpnEnabled(false)
                            val svcIntent = Intent(context, PauseVpnService::class.java)
                                .setAction(PauseVpnService.ACTION_STOP)
                            context.startService(svcIntent)
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Today's stats",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Domains visited: ${uiState.domainsVisited}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Domains blocked: ${uiState.domainsBlocked}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Keyword matches: ${uiState.keywordMatches}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onNavigateToBlacklist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Domain Blacklist")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onNavigateToKeywords,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Keywords")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onNavigateToUrlLog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("URL Visit Log")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onNavigateToWhitelist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Whitelist")
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}
