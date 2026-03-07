package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pause.app.data.db.entity.UnlockEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: UnlockEvent): Long

    @Query(
        """SELECT COUNT(*) FROM unlock_events 
        WHERE unlocked_at >= :midnight"""
    )
    fun getDailyUnlockCount(midnight: Long): Flow<Int>

    @Query(
        """SELECT COUNT(*) FROM unlock_events 
        WHERE unlocked_at >= :since"""
    )
    suspend fun getUnlockCountSince(since: Long): Int

    @Query("DELETE FROM unlock_events WHERE unlocked_at < :before")
    suspend fun deleteOlderThan(before: Long)
}
