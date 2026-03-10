package com.pause.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pause.app.data.AssetImporter
import com.pause.app.data.repository.WebFilterConfigRepository
import com.pause.app.worker.MidnightResetWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PauseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var assetImporter: AssetImporter

    @Inject
    lateinit var webFilterConfigRepository: WebFilterConfigRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleMidnightResetWorker()
        appScope.launch {
            assetImporter.importBundledAssetsIfNeeded()
            if (webFilterConfigRepository.getConfig() == null) {
                webFilterConfigRepository.saveConfig(
                    com.pause.app.data.db.entity.WebFilterConfig(id = 1)
                )
            }
        }
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
