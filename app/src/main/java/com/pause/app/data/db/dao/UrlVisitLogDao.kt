package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pause.app.data.db.entity.UrlVisitLog

@Dao
interface UrlVisitLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UrlVisitLog): Long

    @Query("SELECT * FROM url_visit_log WHERE visited_at >= :since ORDER BY visited_at DESC")
    suspend fun getRecent(since: Long): List<UrlVisitLog>

    @Query("SELECT * FROM url_visit_log WHERE parent_reviewed = 0 ORDER BY visited_at DESC")
    suspend fun getPendingReview(): List<UrlVisitLog>

    @Query("UPDATE url_visit_log SET parent_reviewed = 1 WHERE id = :id")
    suspend fun markReviewed(id: Long)

    @Query(
        """
        SELECT 
            COUNT(*) as visitCount,
            CAST(COALESCE(SUM(CASE WHEN was_blocked = 1 THEN 1 ELSE 0 END), 0) AS INTEGER) as blockedCount,
            CAST(COALESCE(SUM(CASE WHEN classification = 'KEYWORD_MATCH' THEN 1 ELSE 0 END), 0) AS INTEGER) as keywordMatchCount
        FROM url_visit_log 
        WHERE visited_at >= :since
        """
    )
    suspend fun getDailyStats(since: Long): UrlDailyStats

    @Query("DELETE FROM url_visit_log WHERE visited_at < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

data class UrlDailyStats(
    val visitCount: Int,
    val blockedCount: Int,
    val keywordMatchCount: Int
)
