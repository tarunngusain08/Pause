package com.pause.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey val id: Long = 1,
    val currentStreakDays: Int = 0,
    val streakStartedAt: Long = 0,
    val lastValidDay: Long = 0,
    val shieldsRemaining: Int = 1,
    val totalShieldsUsed: Int = 0,
    val longestStreakEver: Int = 0
)
