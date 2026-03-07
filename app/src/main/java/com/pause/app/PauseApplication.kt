package com.pause.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pause.app.worker.MidnightResetWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PauseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleMidnightResetWorker()
    }

    private fun scheduleMidnightResetWorker() {
        val midnightWork = PeriodicWorkRequestBuilder<MidnightResetWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(millisUntilNextMidnight(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MidnightResetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            midnightWork
        )
    }

    private fun millisUntilNextMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return midnight.timeInMillis - now.timeInMillis
    }
}
