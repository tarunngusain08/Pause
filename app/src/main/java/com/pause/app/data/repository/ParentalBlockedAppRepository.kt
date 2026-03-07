package com.pause.app.data.repository

import com.pause.app.data.db.dao.ParentalBlockedAppDao
import com.pause.app.data.db.entity.ParentalBlockedApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalBlockedAppRepository @Inject constructor(
    private val parentalBlockedAppDao: ParentalBlockedAppDao
) {

    fun getActiveBlockedApps(): Flow<List<ParentalBlockedApp>> =
        parentalBlockedAppDao.getActiveBlockedAppsFlow()

    suspend fun getActiveBlockedAppsSync(): List<ParentalBlockedApp> =
        parentalBlockedAppDao.getActiveBlockedApps()

    suspend fun getByPackageName(packageName: String): ParentalBlockedApp? =
        parentalBlockedAppDao.getByPackageName(packageName)

    suspend fun saveBlockedApps(apps: List<ParentalBlockedApp>) {
        parentalBlockedAppDao.replaceAll(apps)
    }
}
