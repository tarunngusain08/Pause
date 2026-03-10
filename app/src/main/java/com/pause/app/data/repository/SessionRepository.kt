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

    suspend fun getActiveFocusSession(): Session? {
        val session = sessionDao.getActiveFocusSession() ?: return null
        if (System.currentTimeMillis() >= session.endsAt) {
            sessionDao.markInactive(session.id)
            return null
        }
        return session
    }

    suspend fun getActiveCommitmentSession(): Session? {
        val session = sessionDao.getActiveCommitmentSession() ?: return null
        if (System.currentTimeMillis() >= session.endsAt) {
            sessionDao.markInactive(session.id)
            return null
        }
        return session
    }

    suspend fun startCommitmentSession(durationMinutes: Int, packageNames: List<String>): Long {
        val now = System.currentTimeMillis()
        val endsAt = now + durationMinutes * 60 * 1000L
        val blockedJson = org.json.JSONArray().apply { packageNames.forEach { put(it) } }.toString()
        val session = Session(
            sessionType = Session.SessionType.COMMITMENT,
            startedAt = now,
            endsAt = endsAt,
            isActive = true,
            blockedPackages = blockedJson
        )
        return sessionDao.insert(session)
    }

    fun isPackageInCommitmentBlockList(session: Session, packageName: String): Boolean {
        val json = session.blockedPackages
        return packageName in parseBlockedPackages(json)
    }

    private fun parseBlockedPackages(json: String): List<String> {
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
