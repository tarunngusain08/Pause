package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey val id: Long = 1,
    @ColumnInfo(name = "current_streak_days") val currentStreakDays: Int = 0,
    @ColumnInfo(name = "streak_started_at") val streakStartedAt: Long = 0,
    @ColumnInfo(name = "last_valid_day") val lastValidDay: Long = 0,
    @ColumnInfo(name = "shields_remaining") val shieldsRemaining: Int = 1,
    @ColumnInfo(name = "total_shields_used") val totalShieldsUsed: Int = 0,
    @ColumnInfo(name = "longest_streak_ever") val longestStreakEver: Int = 0
)
