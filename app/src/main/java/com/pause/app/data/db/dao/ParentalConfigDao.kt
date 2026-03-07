package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pause.app.data.db.entity.ParentalConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentalConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ParentalConfig)

    @Update
    suspend fun update(config: ParentalConfig)

    @Query("SELECT * FROM parental_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): ParentalConfig?

    @Query("SELECT * FROM parental_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<ParentalConfig?>

    @Query("UPDATE parental_config SET emergency_contact_number = :number, emergency_contact_name = :name, last_modified_at = :modifiedAt WHERE id = 1")
    suspend fun updateEmergencyContact(number: String?, name: String?, modifiedAt: Long)

    @Query("UPDATE parental_config SET device_admin_enabled = :enabled, last_modified_at = :modifiedAt WHERE id = 1")
    suspend fun setDeviceAdminEnabled(enabled: Boolean, modifiedAt: Long)

    @Query("UPDATE parental_config SET is_active = :active, last_modified_at = :modifiedAt WHERE id = 1")
    suspend fun setActive(active: Boolean, modifiedAt: Long)
}
