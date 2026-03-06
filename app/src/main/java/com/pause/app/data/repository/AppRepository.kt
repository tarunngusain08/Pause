package com.pause.app.data.repository

import com.pause.app.data.db.dao.MonitoredAppDao
import com.pause.app.data.db.entity.MonitoredApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val monitoredAppDao: MonitoredAppDao
) {

    fun getActiveMonitoredApps(): Flow<List<MonitoredApp>> =
        monitoredAppDao.getActiveMonitoredApps()

    fun getAllMonitoredApps(): Flow<List<MonitoredApp>> =
        monitoredAppDao.getAllMonitoredApps()

    suspend fun getByPackageName(packageName: String): MonitoredApp? =
        monitoredAppDao.getByPackageName(packageName)

    suspend fun isMonitored(packageName: String): Boolean =
        monitoredAppDao.isMonitored(packageName)

    suspend fun addApp(app: MonitoredApp) = monitoredAppDao.insert(app)

    suspend fun updateApp(app: MonitoredApp) = monitoredAppDao.update(app)

    suspend fun removeApp(packageName: String) = monitoredAppDao.deleteByPackageName(packageName)

    suspend fun setMonitoredApps(apps: List<MonitoredApp>) {
        monitoredAppDao.insertAll(apps)
    }
}
