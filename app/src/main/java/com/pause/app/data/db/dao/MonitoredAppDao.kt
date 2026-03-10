package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pause.app.data.db.entity.MonitoredApp
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps WHERE is_active = 1 ORDER BY added_at ASC")
    fun getActiveMonitoredApps(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps ORDER BY added_at ASC")
    fun getAllMonitoredApps(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps ORDER BY added_at ASC")
    suspend fun getAllMonitoredAppsSnapshot(): List<MonitoredApp>

    @Query("SELECT * FROM monitored_apps WHERE package_name = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): MonitoredApp?

    @Query("SELECT (COUNT(*) > 0) FROM monitored_apps WHERE package_name = :packageName AND is_active = 1")
    suspend fun isMonitored(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: MonitoredApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<MonitoredApp>)

    @Update
    suspend fun update(app: MonitoredApp)

    @Delete
    suspend fun delete(app: MonitoredApp)

    @Query("DELETE FROM monitored_apps WHERE package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM monitored_apps")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(apps: List<MonitoredApp>) {
        deleteAll()
        if (apps.isNotEmpty()) insertAll(apps)
    }
}
