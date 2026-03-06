package com.pause.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val appIconUri: String? = null,
    val frictionLevel: FrictionLevel = FrictionLevel.LOW,
    val dailyLaunchLimit: Int? = null,
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
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
