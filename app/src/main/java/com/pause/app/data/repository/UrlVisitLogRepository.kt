package com.pause.app.data.repository

import com.pause.app.data.db.dao.UrlDailyStats
import com.pause.app.data.db.dao.UrlVisitLogDao
import com.pause.app.data.db.entity.UrlVisitLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlVisitLogRepository @Inject constructor(
    private val urlVisitLogDao: UrlVisitLogDao
) {

    suspend fun insert(log: UrlVisitLog): Long =
        urlVisitLogDao.insert(log.copy(fullUrl = log.fullUrl.take(500)))

    suspend fun getRecent(days: Int): List<UrlVisitLog> {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return urlVisitLogDao.getRecent(since)
    }

    suspend fun getPendingReview(): List<UrlVisitLog> =
        urlVisitLogDao.getPendingReview()

    suspend fun markReviewed(id: Long) =
        urlVisitLogDao.markReviewed(id)

    suspend fun getDailyStats(since: Long): UrlDailyStats =
        urlVisitLogDao.getDailyStats(since)

    suspend fun deleteOlderThan(timestamp: Long) =
        urlVisitLogDao.deleteOlderThan(timestamp)
}
