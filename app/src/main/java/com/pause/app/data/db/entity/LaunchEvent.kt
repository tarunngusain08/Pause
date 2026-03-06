package com.pause.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "launch_events",
    foreignKeys = [
        ForeignKey(
            entity = MonitoredApp::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("package_name"), Index("launched_at")]
)
data class LaunchEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val launchedAt: Long,
    val reflectionReason: String? = null,
    val wasCancelled: Boolean = false,
    val wasDuringFocus: Boolean = false,
    val wasDuringCommitment: Boolean = false,
    val delayDurationSeconds: Int = 0
)
