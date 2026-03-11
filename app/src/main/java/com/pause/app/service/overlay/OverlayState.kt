package com.pause.app.service.overlay

enum class OverlayState(val priority: Int, val isInformational: Boolean = false) {
    IDLE(0),
    SHOWING_GENTLE_REENTRY(5, isInformational = true),
    SHOWING_SCHEDULE_RESUME(10, isInformational = true),
    SHOWING_SESSION_RESUME(10, isInformational = true),
    SHOWING_SESSION_COMPLETE(10, isInformational = true),
    SHOWING_LOCK_INTERVENTION(20, isInformational = true),
    SHOWING_REFLECTION(30),
    SHOWING_DELAY(40),
    SHOWING_ALLOWANCE_REACHED(60),
    SHOWING_LAUNCH_LIMIT(65),
    SHOWING_COOLDOWN(70),
    SHOWING_COMMITMENT_BLOCK(85),
    SHOWING_PARENTAL_BLOCK(90),
    SHOWING_POWER_BLOCK(90),
    SHOWING_PIN_ENTRY(95),
    SHOWING_EMERGENCY_CONFIRM(95),
    SHOWING_STRICT_BLOCK(100),
}
