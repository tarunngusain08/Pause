package com.pause.app.service.strict

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import com.pause.app.data.db.entity.Session
import com.pause.app.data.db.entity.StrictBreakLog
import com.pause.app.data.preferences.SessionPreferences
import com.pause.app.data.repository.StrictSessionRepository
import com.pause.app.service.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrictSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val strictSessionRepository: StrictSessionRepository,
    private val overlayManager: OverlayManager,
    private val sessionPreferences: SessionPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeSession = MutableStateFlow<Session?>(null)
    val activeSession: StateFlow<Session?> = _activeSession.asStateFlow()

    init {
        scope.launch {
            loadActiveSession()
        }
    }

    private suspend fun loadActiveSession() {
        withContext(Dispatchers.IO) {
            val session = strictSessionRepository.getActiveStrictSession()
            _activeSession.value = session
        }
    }

    fun getActiveSession(): Session? = _activeSession.value

    suspend fun startSession(durationMs: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val endTimeEpoch = System.currentTimeMillis() + durationMs
        val session = Session(
            sessionType = Session.SessionType.STRICT,
            startedAt = System.currentTimeMillis(),
            endsAt = endTimeEpoch,
            isActive = true,
            blockedPackages = "[]",
            settingsLocked = true
        )
        val id = strictSessionRepository.saveSession(session)
        sessionPreferences.anyStrictActive = true
        _activeSession.value = session.copy(id = id)

        val intent = Intent(context, StrictForegroundService::class.java).apply {
            action = StrictForegroundService.ACTION_START
            putExtra(StrictForegroundService.EXTRA_END_TIME_EPOCH, endTimeEpoch)
        }
        context.startForegroundService(intent)
        Result.success(Unit)
    }

    fun resumeSessionOnBoot() {
        scope.launch { resumeSessionOnBootSync() }
    }

    suspend fun resumeSessionOnBootSync() {
        withContext(Dispatchers.IO) {
            if (!sessionPreferences.anyStrictActive) return@withContext
            val session = strictSessionRepository.getActiveStrictSession() ?: run {
                sessionPreferences.anyStrictActive = false
                return@withContext
            }
            val remainingMs = session.endsAt - System.currentTimeMillis()
            if (remainingMs <= 0) {
                strictSessionRepository.markComplete(session.id)
                strictSessionRepository.logBreak(
                    StrictBreakLog(
                        sessionId = session.id,
                        brokenAt = System.currentTimeMillis(),
                        breakReason = StrictBreakLog.BreakReason.FORCE_RESTART_EXPIRED,
                        remainingMsAtBreak = 0
                    )
                )
                sessionPreferences.anyStrictActive = false
                _activeSession.value = null
                return@withContext
            }
            _activeSession.value = session
            val intent = Intent(context, StrictForegroundService::class.java).apply {
                action = StrictForegroundService.ACTION_START
                putExtra(StrictForegroundService.EXTRA_END_TIME_EPOCH, session.endsAt)
            }
            context.startForegroundService(intent)
        }
    }

    fun getRemainingMs(): Long {
        val session = _activeSession.value ?: return 0
        return (session.endsAt - System.currentTimeMillis()).coerceAtLeast(0)
    }

    fun isPackageBlocked(packageName: String): Boolean {
        _activeSession.value ?: return false
        return packageName !in FOCUS_MODE_ALLOWLIST
    }

    fun isSettingsLocked(): Boolean = _activeSession.value?.settingsLocked == true

    fun onSessionExpired() {
        scope.launch {
            val durationMs = withContext(Dispatchers.IO) {
                val session = _activeSession.value ?: return@withContext null
                strictSessionRepository.markComplete(session.id)
                sessionPreferences.anyStrictActive = false
                _activeSession.value = null

                val stopIntent = Intent(context, StrictForegroundService::class.java).apply {
                    action = StrictForegroundService.ACTION_STOP
                }
                context.startService(stopIntent)

                session.endsAt - session.startedAt
            } ?: return@launch
            overlayManager.showSessionCompleteOverlay(durationMs)
        }
    }

    fun confirmEmergencyExit() {
        scope.launch {
            withContext(Dispatchers.IO) {
                val session = _activeSession.value ?: return@withContext
                val now = System.currentTimeMillis()
                strictSessionRepository.markBroken(session.id, now)
                strictSessionRepository.logBreak(
                    StrictBreakLog(
                        sessionId = session.id,
                        brokenAt = now,
                        breakReason = StrictBreakLog.BreakReason.EMERGENCY_EXIT,
                        remainingMsAtBreak = getRemainingMs()
                    )
                )
                sessionPreferences.anyStrictActive = false
                _activeSession.value = null

                val stopIntent = Intent(context, StrictForegroundService::class.java).apply {
                    action = StrictForegroundService.ACTION_STOP
                }
                context.startService(stopIntent)
            }
        }
    }

    companion object {
        private val FOCUS_MODE_ALLOWLIST = setOf(
            "com.android.dialer",
            "com.android.contacts",
            "com.android.deskclock",
            "com.android.camera",
            "com.android.calculator2",
            "com.android.emergency",
            "com.google.android.dialer",
            "com.google.android.contacts",
            "com.google.android.deskclock",
            "com.google.android.calculator",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.pause.app"
        )
    }
}
