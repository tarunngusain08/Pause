package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pause.app.data.db.entity.LaunchEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface LaunchEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: LaunchEvent): Long

    @Query(
        """SELECT COUNT(*) FROM launch_events 
        WHERE package_name = :packageName AND launched_at >= :midnight AND was_cancelled = 0"""
    )
    fun getTodayLaunchCount(packageName: String, midnight: Long): Flow<Int>

    @Query(
        """SELECT package_name as packageName, COUNT(*) as count FROM launch_events 
        WHERE launched_at >= :midnight AND was_cancelled = 0 
        GROUP BY package_name"""
    )
    fun getTodayLaunchesAll(midnight: Long): Flow<List<LaunchCount>>

    data class LaunchCount(val packageName: String, val count: Int)

    @Query(
        """SELECT * FROM launch_events 
        WHERE launched_at >= :since 
        ORDER BY launched_at DESC"""
    )
    fun getLaunchEventsSince(since: Long): Flow<List<LaunchEvent>>

    @Query(
        """SELECT package_name as packageName, COUNT(*) as count FROM launch_events 
        WHERE launched_at >= :since AND launched_at < :until AND was_cancelled = 0 
        GROUP BY package_name"""
    )
    suspend fun getLaunchCountsBetween(since: Long, until: Long): List<LaunchCount>

    @Query("DELETE FROM launch_events WHERE launched_at < :before")
    suspend fun deleteOlderThan(before: Long)
}
