package com.pause.app.service.parental

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pause.app.data.db.entity.ParentalBlockedApp
import com.pause.app.data.db.entity.ScheduleBandEntity
import com.pause.app.data.preferences.SessionPreferences
import com.pause.app.data.repository.ParentalConfigRepository
import com.pause.app.data.repository.ParentalBlockedAppRepository
import com.pause.app.data.repository.ScheduleRepository
import com.pause.app.service.overlay.OverlayManager
import dagger.hilt.android.qualifiers.ApplicationContext
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
class ParentalControlManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parentalConfigRepository: ParentalConfigRepository,
    private val parentalBlockedAppRepository: ParentalBlockedAppRepository,
    private val scheduleRepository: ScheduleRepository,
    private val scheduleEngine: ScheduleEngine,
    private val pinManager: PINManager,
    private val overlayManager: OverlayManager,
    private val sessionPreferences: SessionPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    init {
        scope.launch {
            parentalConfigRepository.getConfig().collect { config ->
                _isActive.value = config?.isActive == true
            }
        }
    }

    fun isActive(): Boolean = _isActive.value

    suspend fun isActiveSync(): Boolean =
        parentalConfigRepository.getConfigSync()?.isActive == true

    suspend fun getCurrentBand(): ScheduleBandEntity.ScheduleBandType =
        scheduleEngine.getCurrentBand()

    /** Returns true when app should be fully blocked (no access). */
    suspend fun isAppBlocked(packageName: String): Boolean {
        val blockedApp = parentalBlockedAppRepository.getByPackageName(packageName) ?: return false
        val band = getCurrentBand()
        return when (blockedApp.blockType) {
            ParentalBlockedApp.BlockType.ALWAYS -> true
            ParentalBlockedApp.BlockType.SCHEDULE_ONLY ->
                band == ScheduleBandEntity.ScheduleBandType.RESTRICTED
        }
    }

    /** Returns true when app should get friction (delay/reflection) but not full block. */
    suspend fun isAppFrictionRequired(packageName: String): Boolean {
        val blockedApp = parentalBlockedAppRepository.getByPackageName(packageName) ?: return false
        val band = getCurrentBand()
        return blockedApp.blockType == ParentalBlockedApp.BlockType.SCHEDULE_ONLY &&
            band == ScheduleBandEntity.ScheduleBandType.LIMITED
    }

    fun handleAppLaunch(packageName: String, appName: String) {
        scope.launch {
            val (band, blockedApp, config) = withContext(Dispatchers.IO) {
                if (!isActiveSync()) return@launch
                val b = getCurrentBand()
                val app = parentalBlockedAppRepository.getByPackageName(packageName)
                    ?: return@launch
                val cfg = parentalConfigRepository.getConfigSync()
                Triple(b, app, cfg)
            }
            when (band) {
                ScheduleBandEntity.ScheduleBandType.RESTRICTED -> {
                    val liftsAt = withContext(Dispatchers.IO) { formatNextBandChange() }
                    overlayManager.showParentalBlockOverlay(
                        appName = appName,
                        liftsAt = liftsAt,
                        emergencyContact = config?.emergencyContactName,
                        onEmergencyContact = { launchEmergencyContact() },
                        onDismiss = { overlayManager.dismiss() }
                    )
                }
                ScheduleBandEntity.ScheduleBandType.LIMITED -> {
                    if (blockedApp.blockType == ParentalBlockedApp.BlockType.ALWAYS) {
                        val liftsAt = withContext(Dispatchers.IO) { formatNextBandChange() }
                        overlayManager.showParentalBlockOverlay(
                            appName = appName,
                            liftsAt = liftsAt,
                            emergencyContact = config?.emergencyContactName,
                            onEmergencyContact = { launchEmergencyContact() },
                            onDismiss = { overlayManager.dismiss() }
                        )
                    }
                    // else: apply standard friction - handled by AccessibilityService
                }
                ScheduleBandEntity.ScheduleBandType.FREE -> { }
            }
        }
    }

    fun handlePowerLongPress() {
        scope.launch {
            val remainingMs = withContext(Dispatchers.IO) {
                if (!isActiveSync()) return@launch
                scheduleEngine.getNextBandChange()?.msUntilChange ?: 0
            }
            overlayManager.showPowerMenuBlockOverlay(remainingMs)
        }
    }

    suspend fun getTimeUntilNextBandChange(): Long =
        scheduleEngine.getNextBandChange()?.msUntilChange ?: 0L

    private suspend fun formatNextBandChange(): String {
        val change = scheduleEngine.getNextBandChange() ?: return "soon"
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = change.changeAt
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }

    fun launchEmergencyContact() {
        scope.launch {
            val config = parentalConfigRepository.getConfigSync()
            val number = config?.emergencyContactNumber ?: return@launch
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    suspend fun disableParentalControl(pin: String): Boolean {
        val result = pinManager.verifyPIN(pin)
        if (result != PINResult.Correct) return false
        sessionPreferences.parentalActive = false
        parentalConfigRepository.setActive(false)
        return true
    }

    fun resumeOnBoot() {
        scope.launch { resumeOnBootSync() }
    }

    suspend fun resumeOnBootSync() {
        withContext(Dispatchers.IO) {
            if (!sessionPreferences.parentalActive) return@withContext
            val config = parentalConfigRepository.getConfigSync() ?: return@withContext
            if (!config.isActive) return@withContext
            val nextChange = scheduleEngine.getNextBandChange()
            nextChange?.let { scheduleEngine.scheduleNextBandChangeAlarm(it.msUntilChange) }
        }
    }
}
