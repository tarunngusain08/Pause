package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_type") val sessionType: SessionType,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ends_at") val endsAt: Long,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "was_broken") val wasBroken: Boolean = false,
    @ColumnInfo(name = "broken_at") val brokenAt: Long? = null,
    @ColumnInfo(name = "blocked_packages") val blockedPackages: String = "[]",
    @ColumnInfo(name = "settings_locked") val settingsLocked: Boolean = false
) {
    enum class SessionType {
        FOCUS,
        COMMITMENT,
        STRICT
    }
}
