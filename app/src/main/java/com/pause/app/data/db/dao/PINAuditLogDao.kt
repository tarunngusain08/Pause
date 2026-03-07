package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pause.app.data.db.entity.PINAuditLog

@Dao
interface PINAuditLogDao {

    @Insert
    suspend fun insert(log: PINAuditLog): Long

    @Query("SELECT * FROM pin_audit_log ORDER BY attempted_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<PINAuditLog>
}
