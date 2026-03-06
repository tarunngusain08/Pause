package com.pause.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pause.app.worker.MidnightResetWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PauseApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(HiltWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleMidnightResetWorker()
    }

    private fun scheduleMidnightResetWorker() {
        val midnightWork = PeriodicWorkRequestBuilder<MidnightResetWorker>(
            24, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MidnightResetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            midnightWork
        )
    }
}
