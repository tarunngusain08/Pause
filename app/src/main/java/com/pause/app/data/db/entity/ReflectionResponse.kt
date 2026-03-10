package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reflection_responses",
    foreignKeys = [
        ForeignKey(
            entity = LaunchEvent::class,
            parentColumns = ["id"],
            childColumns = ["launch_event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("launch_event_id")]
)
data class ReflectionResponse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "launch_event_id") val launchEventId: Long,
    @ColumnInfo(name = "reason_code") val reasonCode: String,
    @ColumnInfo(name = "responded_at") val respondedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val REASON_BORED = "BORED"
        const val REASON_HABIT = "HABIT"
        const val REASON_REPLYING = "REPLYING"
        const val REASON_INTENTIONAL = "INTENTIONAL"
        const val REASON_TIMED_OUT = "TIMED_OUT"
    }
}
