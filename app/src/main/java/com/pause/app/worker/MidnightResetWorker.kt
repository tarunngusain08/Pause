package com.pause.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pause.app.data.db.dao.PendingReviewDao
import com.pause.app.data.repository.InsightsRepository
import com.pause.app.data.repository.LaunchRepository
import com.pause.app.data.repository.SessionRepository
import com.pause.app.data.repository.UrlVisitLogRepository
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
    private val urlVisitLogRepository: UrlVisitLogRepository,
    private val pendingReviewDao: PendingReviewDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val ninetyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            launchRepository.deleteEventsOlderThan(ninetyDaysAgo)
            insightsRepository.deleteReflectionResponsesOlderThan(ninetyDaysAgo)
            insightsRepository.deleteUnlockEventsOlderThan(ninetyDaysAgo)
            sessionRepository.deleteSessionsOlderThan(ninetyDaysAgo)
            urlVisitLogRepository.deleteOlderThan(thirtyDaysAgo)
            pendingReviewDao.deleteOlderThan(ninetyDaysAgo)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Midnight reset failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "midnight_reset_work"
        private const val TAG = "MidnightResetWorker"
    }
}
