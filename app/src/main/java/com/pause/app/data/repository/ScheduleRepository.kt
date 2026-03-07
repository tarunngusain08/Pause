package com.pause.app.data.repository

import com.pause.app.data.db.dao.ScheduleBandDao
import com.pause.app.data.db.entity.ScheduleBandEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleBandDao: ScheduleBandDao
) {

    suspend fun getBandsForDay(dayOfWeek: Int): List<ScheduleBandEntity> =
        scheduleBandDao.getBandsForDay(dayOfWeek)

    suspend fun getAllBands(): List<ScheduleBandEntity> =
        scheduleBandDao.getAllBands()

    suspend fun saveSchedule(bands: List<ScheduleBandEntity>) {
        scheduleBandDao.replaceAll(bands)
    }
}
