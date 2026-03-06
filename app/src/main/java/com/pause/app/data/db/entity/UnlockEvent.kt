package com.pause.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unlock_events",
    indices = [Index("unlocked_at")]
)
data class UnlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unlockedAt: Long,
    val dailyUnlockCount: Int = 0
)
