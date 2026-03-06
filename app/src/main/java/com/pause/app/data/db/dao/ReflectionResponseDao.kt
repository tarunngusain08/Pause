package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pause.app.data.db.entity.ReflectionResponse

@Dao
interface ReflectionResponseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(response: ReflectionResponse): Long

    @Query(
        """SELECT reason_code as reasonCode, COUNT(*) as count FROM reflection_responses 
        WHERE responded_at >= :since 
        GROUP BY reason_code"""
    )
    suspend fun getReasonCountsSince(since: Long): List<ReasonCount>

    @Query("DELETE FROM reflection_responses WHERE responded_at < :before")
    suspend fun deleteOlderThan(before: Long)

    data class ReasonCount(val reasonCode: String, val count: Int)
}
