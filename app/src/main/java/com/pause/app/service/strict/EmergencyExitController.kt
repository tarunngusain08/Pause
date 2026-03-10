package com.pause.app.service.strict

import javax.inject.Inject
import javax.inject.Singleton

sealed class EmergencyTapResult {
    data class TapRegistered(val count: Int) : EmergencyTapResult()
    object ShowConfirmation : EmergencyTapResult()
}

@Singleton
class EmergencyExitController @Inject constructor() {

    @Volatile private var tapCount = 0
    @Volatile private var firstTapAt = 0L

    @Synchronized
    fun onEmergencyTapped(): EmergencyTapResult {
        val now = System.currentTimeMillis()
        if (tapCount == 0) {
            firstTapAt = now
            tapCount = 1
            return EmergencyTapResult.TapRegistered(1)
        }
        if (!isWithinWindow(now)) {
            reset()
            firstTapAt = now
            tapCount = 1
            return EmergencyTapResult.TapRegistered(1)
        }
        tapCount++
        return if (tapCount >= REQUIRED_TAPS) {
            EmergencyTapResult.ShowConfirmation
        } else {
            EmergencyTapResult.TapRegistered(tapCount)
        }
    }

    @Synchronized
    fun reset() {
        tapCount = 0
        firstTapAt = 0L
    }

    fun getTapCount(): Int = tapCount

    private fun isWithinWindow(now: Long): Boolean =
        (now - firstTapAt) <= TAP_WINDOW_MS

    companion object {
        const val REQUIRED_TAPS = 3
        const val TAP_WINDOW_MS = 5000L
    }
}
