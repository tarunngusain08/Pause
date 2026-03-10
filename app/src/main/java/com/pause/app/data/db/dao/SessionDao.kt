package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pause.app.data.db.entity.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Query("SELECT * FROM sessions WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveSession(): Session?

    @Query("SELECT * FROM sessions WHERE is_active = 1 LIMIT 1")
    fun getActiveSessionFlow(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE session_type = 'STRICT' AND is_active = 1 LIMIT 1")
    suspend fun getActiveStrictSession(): Session?

    @Query("SELECT * FROM sessions WHERE session_type = 'FOCUS' AND is_active = 1 LIMIT 1")
    suspend fun getActiveFocusSession(): Session?

    @Query("SELECT * FROM sessions WHERE session_type = 'COMMITMENT' AND is_active = 1 LIMIT 1")
    suspend fun getActiveCommitmentSession(): Session?

    @Query("UPDATE sessions SET is_active = 0, was_broken = 1, broken_at = :brokenAt, settings_locked = 0 WHERE id = :sessionId")
    suspend fun markBroken(sessionId: Long, brokenAt: Long)

    @Query("UPDATE sessions SET is_active = 0 WHERE id = :sessionId")
    suspend fun markInactive(sessionId: Long)

    @Query("DELETE FROM sessions WHERE ends_at < :before")
    suspend fun deleteOlderThan(before: Long)
}
