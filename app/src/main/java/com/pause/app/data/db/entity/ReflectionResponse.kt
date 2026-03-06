package com.pause.app.data.db.entity

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
            childColumns = ["launchEventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("launch_event_id")]
)
data class ReflectionResponse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val launchEventId: Long,
    val reasonCode: String,
    val respondedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val REASON_BORED = "BORED"
        const val REASON_HABIT = "HABIT"
        const val REASON_REPLYING = "REPLYING"
        const val REASON_INTENTIONAL = "INTENTIONAL"
    }
}
