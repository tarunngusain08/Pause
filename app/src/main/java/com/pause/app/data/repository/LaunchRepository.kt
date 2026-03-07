package com.pause.app.data.repository

import com.pause.app.data.db.dao.LaunchEventDao
import com.pause.app.data.db.entity.LaunchEvent
import com.pause.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LaunchRepository @Inject constructor(
    private val launchEventDao: LaunchEventDao
) {

    fun getTodayLaunches(packageName: String): Flow<Int> {
        val midnight = DateUtils.getTodayMidnight()
        return launchEventDao.getTodayLaunchCount(packageName, midnight)
    }

    fun getTodayLaunchesAll(): Flow<Map<String, Int>> {
        val midnight = DateUtils.getTodayMidnight()
        return launchEventDao.getTodayLaunchesAll(midnight).map { list ->
            list.associate { it.packageName to it.count }
        }
    }

    fun getLaunchEventsSince(since: Long): Flow<List<LaunchEvent>> =
        launchEventDao.getLaunchEventsSince(since)

    suspend fun recordLaunch(event: LaunchEvent): Long = launchEventDao.insert(event)

    suspend fun deleteEventsOlderThan(before: Long) = launchEventDao.deleteOlderThan(before)
}
