package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pause.app.data.db.entity.StrictBreakLog

@Dao
interface StrictBreakLogDao {

    @Insert
    suspend fun insert(breakLog: StrictBreakLog): Long

    @Query("SELECT * FROM strict_break_log WHERE session_id = :sessionId ORDER BY broken_at DESC")
    suspend fun getBySessionId(sessionId: Long): List<StrictBreakLog>
}
