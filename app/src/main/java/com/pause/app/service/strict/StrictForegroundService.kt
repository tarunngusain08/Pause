package com.pause.app.service.strict

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pause.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that holds the Strict Mode countdown timer and displays a persistent
 * notification with remaining time. Survives app process loss.
 */
@AndroidEntryPoint
class StrictForegroundService : Service() {

    @Inject
    lateinit var strictSessionManager: StrictSessionManager

    private var countDownTimer: CountDownTimer? = null
    private var notificationManager: NotificationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val endTimeEpoch = intent.getLongExtra(EXTRA_END_TIME_EPOCH, 0L)
                if (endTimeEpoch > 0) {
                    startWithSession(endTimeEpoch)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroy()
    }

    private fun startWithSession(endTimeEpoch: Long) {
        val remainingMs = endTimeEpoch - System.currentTimeMillis()
        if (remainingMs <= 0) {
            strictSessionManager.onSessionExpired()
            stopSelf()
            return
        }

        createNotificationChannel()

        startForeground(NOTIFICATION_ID, buildNotification(remainingMs))

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateNotification(millisUntilFinished)
            }

            override fun onFinish() {
                strictSessionManager.onSessionExpired()
                stopSelf()
            }
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.strict_mode_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(remainingMs: Long): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.strict_mode_notification_title))
            .setContentText(formatCountdown(remainingMs))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(remainingMs: Long) {
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(remainingMs))
    }

    private fun formatCountdown(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d remaining", minutes, seconds)
    }

    companion object {
        private const val CHANNEL_ID = "strict_mode_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.pause.app.STRICT_SERVICE_START"
        const val ACTION_STOP = "com.pause.app.STRICT_SERVICE_STOP"
        const val EXTRA_END_TIME_EPOCH = "end_time_epoch"

    }
}
