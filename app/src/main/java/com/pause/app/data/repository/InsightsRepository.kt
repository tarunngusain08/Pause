package com.pause.app.data.repository

import com.pause.app.data.db.dao.LaunchEventDao
import com.pause.app.data.db.dao.ReflectionResponseDao
import com.pause.app.data.db.dao.UnlockEventDao
import com.pause.app.data.db.entity.LaunchEvent
import com.pause.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsRepository @Inject constructor(
    private val launchEventDao: LaunchEventDao,
    private val reflectionResponseDao: ReflectionResponseDao,
    private val unlockEventDao: UnlockEventDao
) {

    fun getLaunchEventsSince(since: Long): Flow<List<LaunchEvent>> =
        launchEventDao.getLaunchEventsSince(since)

    suspend fun getReflectionReasonCountsSince(
        since: Long
    ): List<ReflectionResponseDao.ReasonCount> =
        reflectionResponseDao.getReasonCountsSince(since)

    fun getDailyUnlockCount(): Flow<Int> {
        val midnight = DateUtils.getTodayMidnight()
        return unlockEventDao.getDailyUnlockCount(midnight)
    }

    suspend fun getUnlockCountSince(since: Long): Int =
        unlockEventDao.getUnlockCountSince(since)

    suspend fun deleteReflectionResponsesOlderThan(before: Long) =
        reflectionResponseDao.deleteOlderThan(before)

    suspend fun deleteUnlockEventsOlderThan(before: Long) =
        unlockEventDao.deleteOlderThan(before)
}
