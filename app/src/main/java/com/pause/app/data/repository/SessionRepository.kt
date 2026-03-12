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

    fun getActiveFocusSessionFlow(): Flow<Session?> = sessionDao.getActiveFocusSessionFlow()

    suspend fun insertSession(session: Session): Long = sessionDao.insert(session)

    suspend fun updateSession(session: Session) = sessionDao.update(session)

    suspend fun markSessionBroken(sessionId: Long, brokenAt: Long) =
        sessionDao.markBroken(sessionId, brokenAt)

    suspend fun markSessionInactive(sessionId: Long) = sessionDao.markInactive(sessionId)

    suspend fun deleteSessionsOlderThan(before: Long) = sessionDao.deleteOlderThan(before)

    suspend fun startFocusSession(durationMinutes: Int): Long {
        val now = System.currentTimeMillis()
        val endsAt = now + durationMinutes * 60 * 1000L
        val session = Session(
            sessionType = Session.SessionType.FOCUS,
            startedAt = now,
            endsAt = endsAt,
            isActive = true,
            blockedPackages = "[]"
        )
        return sessionDao.insert(session)
    }

    /**
     * Returns the active focus session, or null if none exists or the session has expired.
     * NOTE: intentionally marks expired sessions as inactive as a lazy expiry mechanism;
     * callers should be aware this is a read-with-side-effect.
     */
    suspend fun getActiveFocusSession(): Session? {
        val session = sessionDao.getActiveFocusSession() ?: return null
        if (System.currentTimeMillis() >= session.endsAt) {
            sessionDao.markInactive(session.id)
            return null
        }
        return session
    }
}
