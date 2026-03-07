package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "launch_events",
    foreignKeys = [
        ForeignKey(
            entity = MonitoredApp::class,
            parentColumns = ["package_name"],
            childColumns = ["package_name"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("package_name"), Index("launched_at")]
)
data class LaunchEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "launched_at") val launchedAt: Long,
    @ColumnInfo(name = "reflection_reason") val reflectionReason: String? = null,
    @ColumnInfo(name = "was_cancelled") val wasCancelled: Boolean = false,
    @ColumnInfo(name = "was_during_focus") val wasDuringFocus: Boolean = false,
    @ColumnInfo(name = "was_during_commitment") val wasDuringCommitment: Boolean = false,
    @ColumnInfo(name = "delay_duration_seconds") val delayDurationSeconds: Int = 0
)
