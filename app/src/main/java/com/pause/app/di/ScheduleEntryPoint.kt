package com.pause.app.di

import com.pause.app.service.parental.ScheduleEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Entry point for ScheduleBandChangeReceiver - provides ScheduleEngine for alarm rescheduling. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScheduleEntryPoint {
    fun getScheduleEngine(): ScheduleEngine
}
