package com.pause.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionType: SessionType,
    val startedAt: Long,
    val endsAt: Long,
    val isActive: Boolean = true,
    val wasBroken: Boolean = false,
    val brokenAt: Long? = null,
    val blockedPackages: String = "[]"
) {
    enum class SessionType {
        FOCUS,
        COMMITMENT
    }
}
