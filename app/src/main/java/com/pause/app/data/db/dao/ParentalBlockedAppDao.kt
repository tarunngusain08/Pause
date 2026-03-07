package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pause.app.data.db.entity.ParentalBlockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentalBlockedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ParentalBlockedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<ParentalBlockedApp>)

    @Update
    suspend fun update(app: ParentalBlockedApp)

    @Query("SELECT * FROM parental_blocked_apps WHERE is_active = 1")
    suspend fun getActiveBlockedApps(): List<ParentalBlockedApp>

    @Query("SELECT * FROM parental_blocked_apps WHERE is_active = 1")
    fun getActiveBlockedAppsFlow(): Flow<List<ParentalBlockedApp>>

    @Query("SELECT * FROM parental_blocked_apps WHERE package_name = :packageName AND is_active = 1 LIMIT 1")
    suspend fun getByPackageName(packageName: String): ParentalBlockedApp?

    @Query("DELETE FROM parental_blocked_apps")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(apps: List<ParentalBlockedApp>) {
        deleteAll()
        if (apps.isNotEmpty()) insertAll(apps)
    }
}
