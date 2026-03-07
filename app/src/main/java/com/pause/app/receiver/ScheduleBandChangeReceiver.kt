package com.pause.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pause.app.di.ScheduleEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Receives AlarmManager broadcasts when a schedule band change occurs.
 * Reschedules the next band-change alarm so parental schedule continues correctly.
 * Uses goAsync() to avoid ANR from runBlocking on the main thread.
 */
class ScheduleBandChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SCHEDULE_BAND_CHANGE) return
        Log.d(TAG, "Schedule band change alarm fired, rescheduling next change")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                runBlocking {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        ScheduleEntryPoint::class.java
                    )
                    val nextChange = entryPoint.getScheduleEngine().getNextBandChange()
                    nextChange?.let {
                        entryPoint.getScheduleEngine().scheduleNextBandChangeAlarm(it.msUntilChange)
                        Log.d(TAG, "Next band change scheduled in ${it.msUntilChange}ms")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule band change alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SCHEDULE_BAND_CHANGE = "com.pause.app.SCHEDULE_BAND_CHANGE"
        private const val TAG = "ScheduleBandChangeReceiver"
    }
}
