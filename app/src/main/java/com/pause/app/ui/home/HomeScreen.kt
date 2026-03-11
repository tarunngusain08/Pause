package com.pause.app.ui.home

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay


@Composable
fun HomeScreen(
    onNavigateToAppSelection: () -> Unit,
    onNavigateToStrictSetup: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToContentShield: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val monitoredApps by viewModel.monitoredApps.collectAsState(initial = emptyList())
    val todayLaunches by viewModel.todayLaunches.collectAsState(initial = emptyMap())
    val streak by viewModel.streak.collectAsState(initial = null)
    val costOfScrollMinutes by viewModel.costOfScrollMinutes.collectAsState(initial = null)
    val strictSession by viewModel.strictSession.collectAsState(initial = null)
    val focusSession by viewModel.focusSession.collectAsStateWithLifecycle()
    val streakExpiresInMs by viewModel.streakExpiresInMs.collectAsStateWithLifecycle()
    val remainingAllowance by viewModel.remainingAllowanceMinutes.collectAsState(initial = null)
    val unlocksToday by viewModel.unlocksToday.collectAsState(initial = 0)
    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var strictRemainingMs by remember { mutableStateOf(0L) }
    var focusRemainingMs by remember { mutableStateOf(0L) }
    // Tracks whether the user explicitly dismissed the permission dialog this session.
    // rememberSaveable survives configuration changes (rotation) so the dialog doesn't reappear.
    var permissionDialogDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(strictSession) {
        if (strictSession != null) {
            strictRemainingMs = viewModel.getStrictRemainingMs()
            while (strictSession != null) {
                strictRemainingMs = viewModel.getStrictRemainingMs()
                delay(1000)
            }
        }
    }

    LaunchedEffect(focusSession) {
        while (focusSession != null) {
            val s = focusSession ?: break
            focusRemainingMs = (s.endsAt - System.currentTimeMillis()).coerceAtLeast(0)
            delay(1000)
        }
    }

    LaunchedEffect(commitmentSession) {
        while (commitmentSession != null) {
            val s = commitmentSession ?: break
            commitmentRemainingMs = (s.endsAt - System.currentTimeMillis()).coerceAtLeast(0)
            delay(1000)
        }
    }

    // Re-check permissions every time the screen becomes visible (ON_RESUME).
    // Also reset dismiss flag so returning after fixing in Settings re-evaluates.
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

    // Show permission dialog if anything is still missing and user hasn't dismissed it yet.
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
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Pause",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Strict Mode active card
        strictSession?.let {
            val minutes = (strictRemainingMs / 1000 / 60).toInt()
            val seconds = ((strictRemainingMs / 1000) % 60).toInt()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Strict Mode active",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%d:%02d remaining", minutes, seconds),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Focus Mode active card
        focusSession?.let {
            val minutes = (focusRemainingMs / 1000 / 60).toInt()
            val seconds = ((focusRemainingMs / 1000) % 60).toInt()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Focus Mode active",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%d:%02d remaining", minutes, seconds),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Commitment Mode active card
        commitmentSession?.let {
            val minutes = (commitmentRemainingMs / 1000 / 60).toInt()
            val seconds = ((commitmentRemainingMs / 1000) % 60).toInt()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Commitment Mode active",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%d:%02d remaining", minutes, seconds),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Cost of a Scroll card (Phase 2+)
        costOfScrollMinutes?.let { minutes ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cost of a Scroll",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Yesterday you opened monitored apps ~$minutes min worth of times.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    TextButton(onClick = { viewModel.dismissCostOfScrollCard() }) {
                        Text("Dismiss")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Daily allowance remaining (Phase 2+)
        remainingAllowance?.let { remaining ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily allowance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$remaining minutes remaining",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Streak card
        streak?.let { s ->
            val todayMidnight = com.pause.app.util.DateUtils.getTodayMidnight()
            val streakAtRisk = s.currentStreakDays > 0 && s.lastValidDay < todayMidnight
            val milestones = listOf(7, 14, 30, 60, 100)
            val nextMilestone = milestones.firstOrNull { it > s.currentStreakDays }
            val contentColor = if (streakAtRisk)
                MaterialTheme.colorScheme.onErrorContainer
            else
                MaterialTheme.colorScheme.onTertiaryContainer
            val arcColor = if (streakAtRisk)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.tertiary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (streakAtRisk)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular progress arc around the streak count
                    val sweepAngle = if (nextMilestone != null && nextMilestone > 0) {
                        (s.currentStreakDays.toFloat() / nextMilestone.toFloat()) * 360f
                    } else 360f
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(72.dp)) {
                            drawArc(
                                color = arcColor.copy(alpha = 0.2f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "\uD83D\uDD25",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (streakAtRisk) "Streak at risk!" else "Streak",
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${s.currentStreakDays} days",
                            style = MaterialTheme.typography.headlineSmall,
                            color = contentColor
                        )
                        if (streakAtRisk) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val expiresMs = streakExpiresInMs
                            if (expiresMs != null) {
                                val hoursLeft = (expiresMs / 3_600_000).toInt()
                                val minsLeft = ((expiresMs % 3_600_000) / 60_000).toInt()
                                Text(
                                    text = "Expires in ${hoursLeft}h ${minsLeft}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor
                                )
                            } else {
                                Text(
                                    text = "Use Pause today to keep your streak alive!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor
                                )
                            }
                        }
                        nextMilestone?.let { milestone ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${milestone - s.currentStreakDays} days to $milestone-day milestone",
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Unlocks today (Phase 3+)
        if (unlocksToday > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$unlocksToday unlocks today",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Monitored apps section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monitored Apps",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${monitoredApps.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (monitoredApps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No apps monitored yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap \"Add / Edit Apps\" below to choose which apps Pause will intercept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            monitoredApps.forEach { app ->
                val count = todayLaunches[app.packageName] ?: 0
                val limit = app.dailyLaunchLimit
                val countText = if (limit != null) {
                    "$count / $limit opens today"
                } else {
                    "$count opens today"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToAppSelection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add / Edit Apps")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onNavigateToStrictSetup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Strict Mode")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (focusSession == null) {
            OutlinedButton(
                onClick = onNavigateToFocus,
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = onNavigateToInsights,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Weekly Insights")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (commitmentSession == null) {
            OutlinedButton(
                onClick = onNavigateToCommitment,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Commitment Mode")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onNavigateToContentShield,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Content Shield")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings")
        }
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
                    text = "Pause needs the following permissions to work correctly:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (overlayMissing) {
                    PermissionRow(
                        icon = Icons.Outlined.Layers,
                        title = "Display over other apps",
                        description = "Required to show the pause screen when you open a monitored app.",
                        actionLabel = "Open Settings",
                        onAction = onFixOverlay
                    )
                }
                if (accessibilityMissing) {
                    PermissionRow(
                        icon = Icons.Outlined.Accessibility,
                        title = "Accessibility service",
                        description = "Required to detect when a monitored app is launched.",
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
