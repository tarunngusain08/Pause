package com.pause.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pause.app.MainActivity
import com.pause.app.R
import com.pause.app.data.repository.ParentalConfigRepository
import com.pause.app.receiver.ScheduleBandChangeReceiver
import com.pause.app.service.parental.ScheduleEngine
import com.pause.app.service.webfilter.PauseVpnService
import com.pause.app.util.PermissionHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs every 6 hours to verify that critical Pause services are healthy.
 * Posts a user-facing notification if accessibility service, VPN, or parental schedule alarm is not running.
 */
@HiltWorker
class HealthCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val parentalConfigRepository: ParentalConfigRepository,
    private val scheduleEngine: ScheduleEngine
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val issues = mutableListOf<String>()

            if (!PermissionHelper.hasAccessibilityServiceEnabled(appContext)) {
                issues.add("Accessibility service is disabled")
            }

            val heartbeatPrefs = appContext.getSharedPreferences(
                PauseVpnService.HEARTBEAT_PREFS,
                Context.MODE_PRIVATE
            )
            val lastHeartbeat = heartbeatPrefs.getLong(PauseVpnService.KEY_LAST_HEARTBEAT, 0L)
            val heartbeatAge = System.currentTimeMillis() - lastHeartbeat
            if (lastHeartbeat > 0 && heartbeatAge > 2 * 60 * 60 * 1000L) {
                issues.add("Web filter may have stopped")
            }

            val parentalConfig = parentalConfigRepository.getConfigSync()
            if (parentalConfig?.isActive == true) {
                val alarmIntent = Intent(appContext, ScheduleBandChangeReceiver::class.java).apply {
                    action = ScheduleBandChangeReceiver.ACTION_SCHEDULE_BAND_CHANGE
                }
                val existingAlarm = PendingIntent.getBroadcast(
                    appContext, 0, alarmIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (existingAlarm == null) {
                    val nextChange = scheduleEngine.getNextBandChange()
                    nextChange?.let { scheduleEngine.scheduleNextBandChangeAlarm(it.msUntilChange) }
                    issues.add("Parental schedule alarm was not registered — re-scheduled")
                }
            }

            if (issues.isNotEmpty()) {
                postHealthNotification(issues)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "HealthCheckWorker failed", e)
            Result.retry()
        }
    }

    private fun postHealthNotification(issues: List<String>) {
        val nm = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Focus Health", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = issues.joinToString("\n")
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus needs attention")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "health_check_work"
        private const val CHANNEL_ID = "pause_health"
        private const val NOTIFICATION_ID = 3001
        private const val TAG = "HealthCheckWorker"
    }
}
