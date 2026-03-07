package com.pause.app.di

import com.pause.app.service.parental.ParentalControlManager
import com.pause.app.service.strict.StrictSessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Entry point for BootReceiver - provides components needed for boot resume. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootEntryPoint {
    fun getStrictSessionManager(): StrictSessionManager
    fun getParentalControlManager(): ParentalControlManager
}
