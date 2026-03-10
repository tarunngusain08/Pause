package com.pause.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pause.app.data.db.dao.PendingReviewDao
import com.pause.app.data.repository.AppRepository
import com.pause.app.data.repository.InsightsRepository
import com.pause.app.data.repository.LaunchRepository
import com.pause.app.data.repository.SessionRepository
import com.pause.app.data.repository.StreakRepository
import com.pause.app.data.repository.UrlVisitLogRepository
import com.pause.app.util.DateUtils
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
    private val pendingReviewDao: PendingReviewDao,
    private val appRepository: AppRepository,
    private val streakRepository: StreakRepository
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
            evaluateStreak()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Midnight reset failed", e)
            Result.retry()
        }
    }

    private suspend fun evaluateStreak() {
        val yesterdayStart = DateUtils.getYesterdayMidnight()
        val yesterdayCounts = launchRepository.getYesterdayLaunchCountsPerPackage()
        val monitoredApps = appRepository.getActiveMonitoredAppsSnapshot()
        val exceededLimit = monitoredApps.any { app ->
            val limit = app.dailyLaunchLimit ?: return@any false
            val count = yesterdayCounts[app.packageName] ?: 0
            count > limit
        }
        val currentStreak = streakRepository.getStreak()
            ?: com.pause.app.data.db.entity.Streak(id = 1)
        if (exceededLimit) {
            if (currentStreak.shieldsRemaining > 0) {
                streakRepository.insertOrUpdateStreak(
                    currentStreak.copy(shieldsRemaining = currentStreak.shieldsRemaining - 1)
                )
            } else {
                streakRepository.insertOrUpdateStreak(
                    currentStreak.copy(
                        currentStreakDays = 0,
                        streakStartedAt = 0,
                        lastValidDay = yesterdayStart
                    )
                )
            }
        } else {
            val newDays = currentStreak.currentStreakDays + 1
            val longest = maxOf(currentStreak.longestStreakEver, newDays)
            streakRepository.insertOrUpdateStreak(
                currentStreak.copy(
                    currentStreakDays = newDays,
                    streakStartedAt = if (currentStreak.streakStartedAt == 0L) yesterdayStart else currentStreak.streakStartedAt,
                    lastValidDay = yesterdayStart,
                    longestStreakEver = longest
                )
            )
        }
    }

    companion object {
        const val WORK_NAME = "midnight_reset_work"
        private const val TAG = "MidnightResetWorker"
    }
}
