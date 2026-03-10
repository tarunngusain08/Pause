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

    private suspend fun ensureConfigExists() {
        if (parentalConfigDao.getConfig() == null) {
            parentalConfigDao.insert(ParentalConfig(id = 1))
        }
    }

    suspend fun updateEmergencyContact(number: String?, name: String?) {
        ensureConfigExists()
        parentalConfigDao.updateEmergencyContact(number, name, System.currentTimeMillis())
    }

    suspend fun setDeviceAdminEnabled(enabled: Boolean) {
        ensureConfigExists()
        parentalConfigDao.setDeviceAdminEnabled(enabled, System.currentTimeMillis())
    }

    suspend fun setActive(active: Boolean) {
        ensureConfigExists()
        parentalConfigDao.setActive(active, System.currentTimeMillis())
    }
}
