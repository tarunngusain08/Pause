package com.pause.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pause.app.di.BootEntryPoint
import com.pause.app.service.webfilter.PauseVpnService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Handles BOOT_COMPLETED and LOCKED_BOOT_COMPLETED to resume Strict Mode or Parental Control
 * sessions after device restart. Also restarts VPN if web filter was enabled.
 * Uses goAsync() to prevent process kill before work completes.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        runBlocking {
                            val entryPoint = EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                BootEntryPoint::class.java
                            )
                            try {
                                entryPoint.getStrictSessionManager().resumeSessionOnBootSync()
                                entryPoint.getParentalControlManager().resumeOnBootSync()
                                val config = entryPoint.getWebFilterConfigRepository().getConfig()
                                if (config != null && config.vpnEnabled) {
                                    val vpnIntent = Intent(context, PauseVpnService::class.java)
                                        .setAction(PauseVpnService.ACTION_START)
                                    context.startForegroundService(vpnIntent)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Boot recovery failed", e)
                                postBootFailureNotification(context)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun postBootFailureNotification(context: Context) {
        val channelId = "pause_boot_error"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Pause Errors", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = if (launchIntent != null) {
            PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Pause needs attention")
            .setContentText("Boot recovery failed. Please open Pause to restore your session.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { if (pi != null) setContentIntent(pi) }
            .build()
        nm.notify(BOOT_ERROR_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val BOOT_ERROR_NOTIFICATION_ID = 9001
    }
}
