package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parental_config")
data class ParentalConfig(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "emergency_contact_number") val emergencyContactNumber: String? = null,
    @ColumnInfo(name = "emergency_contact_name") val emergencyContactName: String? = null,
    @ColumnInfo(name = "device_admin_enabled") val deviceAdminEnabled: Boolean = false,
    @ColumnInfo(name = "setup_at") val setupAt: Long = 0L,
    @ColumnInfo(name = "last_modified_at") val lastModifiedAt: Long = 0L
)
