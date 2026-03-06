package com.pause.app.data.repository

import com.pause.app.data.db.dao.LaunchEventDao
import com.pause.app.data.db.dao.ReflectionResponseDao
import com.pause.app.data.db.dao.UnlockEventDao
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsRepository @Inject constructor(
    private val launchEventDao: LaunchEventDao,
    private val reflectionResponseDao: ReflectionResponseDao,
    private val unlockEventDao: UnlockEventDao
) {

    fun getLaunchEventsSince(since: Long): Flow<List<com.pause.app.data.db.entity.LaunchEvent>> =
        launchEventDao.getLaunchEventsSince(since)

    suspend fun getReflectionReasonCountsSince(since: Long): List<ReflectionResponseDao.ReasonCount> =
        reflectionResponseDao.getReasonCountsSince(since)

    fun getDailyUnlockCount(): Flow<Int> {
        val midnight = getTodayMidnight()
        return unlockEventDao.getDailyUnlockCount(midnight)
    }

    suspend fun getUnlockCountSince(since: Long): Int = unlockEventDao.getUnlockCountSince(since)

    suspend fun deleteReflectionResponsesOlderThan(before: Long) =
        reflectionResponseDao.deleteOlderThan(before)

    fun getTodayMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getWeekAgoMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getMonthAgoMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
