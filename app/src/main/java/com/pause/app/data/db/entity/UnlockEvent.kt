package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unlock_events",
    indices = [Index("unlocked_at")]
)
data class UnlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long,
    @ColumnInfo(name = "daily_unlock_count") val dailyUnlockCount: Int = 0
)
