package com.pause.app.service.overlay

enum class OverlayState(val priority: Int, val isInformational: Boolean = false) {
    IDLE(0),
    SHOWING_SCHEDULE_RESUME(10, isInformational = true),
    SHOWING_SESSION_RESUME(10, isInformational = true),
    SHOWING_SESSION_COMPLETE(10, isInformational = true),
    SHOWING_LOCK_INTERVENTION(20, isInformational = true),
    SHOWING_PARENTAL_BLOCK(90),
    SHOWING_POWER_BLOCK(90),
    SHOWING_PIN_ENTRY(95),
    SHOWING_EMERGENCY_CONFIRM(95),
    SHOWING_CONTENT_SHIELD_BLOCK(88),
    SHOWING_STRICT_BLOCK(100),
}
