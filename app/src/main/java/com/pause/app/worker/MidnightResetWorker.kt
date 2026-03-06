package com.pause.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pause.app.data.repository.InsightsRepository
import com.pause.app.data.repository.LaunchRepository
import com.pause.app.data.repository.SessionRepository
import com.pause.app.data.repository.StreakRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class MidnightResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val launchRepository: LaunchRepository,
    private val insightsRepository: InsightsRepository,
    private val sessionRepository: SessionRepository,
    private val streakRepository: StreakRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val ninetyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            launchRepository.deleteEventsOlderThan(ninetyDaysAgo)
            insightsRepository.deleteReflectionResponsesOlderThan(ninetyDaysAgo)
            sessionRepository.deleteSessionsOlderThan(ninetyDaysAgo)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "midnight_reset_work"
    }
}
