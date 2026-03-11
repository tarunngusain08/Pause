package com.pause.app.service.overlay

/** Implemented by overlay views that own a CountDownTimer. */
interface TimerCancellable {
    fun cancelTimers()
}
