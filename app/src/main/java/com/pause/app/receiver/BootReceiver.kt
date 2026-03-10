package com.pause.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                            entryPoint.getStrictSessionManager().resumeSessionOnBootSync()
                            entryPoint.getParentalControlManager().resumeOnBootSync()
                            val config = entryPoint.getWebFilterConfigRepository().getConfig()
                            if (config != null && config.vpnEnabled) {
                                val vpnIntent = Intent(context, PauseVpnService::class.java)
                                    .setAction(PauseVpnService.ACTION_START)
                                context.startForegroundService(vpnIntent)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
