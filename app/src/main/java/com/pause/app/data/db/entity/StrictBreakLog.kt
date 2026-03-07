package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "strict_break_log",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class StrictBreakLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "broken_at") val brokenAt: Long,
    @ColumnInfo(name = "break_reason") val breakReason: BreakReason,
    @ColumnInfo(name = "remaining_ms_at_break") val remainingMsAtBreak: Long
) {
    enum class BreakReason {
        EMERGENCY_EXIT,
        FORCE_RESTART_EXPIRED
    }
}
