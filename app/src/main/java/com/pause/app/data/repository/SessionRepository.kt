package com.pause.app.data.repository

import com.pause.app.data.db.dao.SessionDao
import com.pause.app.data.db.entity.Session
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {

    suspend fun getActiveSession(): Session? = sessionDao.getActiveSession()

    fun getActiveSessionFlow(): Flow<Session?> = sessionDao.getActiveSessionFlow()

    suspend fun insertSession(session: Session): Long = sessionDao.insert(session)

    suspend fun updateSession(session: Session) = sessionDao.update(session)

    suspend fun markSessionBroken(sessionId: Long, brokenAt: Long) =
        sessionDao.markBroken(sessionId, brokenAt)

    suspend fun markSessionInactive(sessionId: Long) = sessionDao.markInactive(sessionId)

    suspend fun deleteSessionsOlderThan(before: Long) = sessionDao.deleteOlderThan(before)
}
