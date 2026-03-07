package com.pause.app.data.repository

import com.pause.app.data.db.dao.SessionDao
import com.pause.app.data.db.dao.StrictBreakLogDao
import com.pause.app.data.db.entity.Session
import com.pause.app.data.db.entity.StrictBreakLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrictSessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val strictBreakLogDao: StrictBreakLogDao
) {

    suspend fun getActiveStrictSession(): Session? = sessionDao.getActiveStrictSession()

    suspend fun saveSession(session: Session): Long = sessionDao.insert(session)

    suspend fun markBroken(sessionId: Long, brokenAt: Long) {
        sessionDao.markBroken(sessionId, brokenAt)
    }

    suspend fun markComplete(sessionId: Long) {
        sessionDao.markInactive(sessionId)
    }

    suspend fun logBreak(breakLog: StrictBreakLog) {
        strictBreakLogDao.insert(breakLog)
    }
}
