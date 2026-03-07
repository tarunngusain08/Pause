package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parental_blocked_apps")
data class ParentalBlockedApp(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "block_type") val blockType: BlockType,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
) {
    enum class BlockType {
        ALWAYS,
        SCHEDULE_ONLY
    }
}
