package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pause.app.data.db.entity.PendingReview
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: PendingReview): Long

    @Query("SELECT * FROM pending_review ORDER BY flagged_at DESC")
    fun getAll(): Flow<List<PendingReview>>

    @Query("SELECT * FROM pending_review WHERE status = 'PENDING' ORDER BY flagged_at DESC")
    suspend fun getPending(): List<PendingReview>

    @Query("UPDATE pending_review SET status = :status, resolved_at = :resolvedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, resolvedAt: Long?)
}
