package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "app_icon_uri") val appIconUri: String? = null,
    @ColumnInfo(name = "friction_level") val frictionLevel: FrictionLevel = FrictionLevel.LOW,
    @ColumnInfo(name = "daily_launch_limit") val dailyLaunchLimit: Int? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
) {
    enum class FrictionLevel(val value: Int) {
        LOW(0),
        MEDIUM(1),
        HIGH(2),
        COMMITMENT(3);

        companion object {
            fun fromValue(value: Int): FrictionLevel =
                entries.find { it.value == value } ?: LOW
        }
    }
}
