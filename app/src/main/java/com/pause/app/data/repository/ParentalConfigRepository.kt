package com.pause.app.data.repository

import com.pause.app.data.db.dao.ParentalConfigDao
import com.pause.app.data.db.entity.ParentalConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalConfigRepository @Inject constructor(
    private val parentalConfigDao: ParentalConfigDao
) {

    fun getConfig(): Flow<ParentalConfig?> = parentalConfigDao.getConfigFlow()

    suspend fun getConfigSync(): ParentalConfig? = parentalConfigDao.getConfig()

    suspend fun saveConfig(config: ParentalConfig) {
        parentalConfigDao.insert(config)
    }

    suspend fun updateEmergencyContact(number: String?, name: String?) {
        if (parentalConfigDao.getConfig() == null) return
        parentalConfigDao.updateEmergencyContact(number, name, System.currentTimeMillis())
    }

    suspend fun setDeviceAdminEnabled(enabled: Boolean) {
        parentalConfigDao.setDeviceAdminEnabled(enabled, System.currentTimeMillis())
    }

    suspend fun setActive(active: Boolean) {
        parentalConfigDao.setActive(active, System.currentTimeMillis())
    }
}
