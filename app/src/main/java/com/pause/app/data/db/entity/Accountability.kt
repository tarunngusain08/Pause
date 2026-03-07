package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accountability")
data class Accountability(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "partner_contact") val partnerContact: String,
    @ColumnInfo(name = "partner_name") val partnerName: String,
    @ColumnInfo(name = "partner_accepted") val partnerAccepted: Boolean = false,
    @ColumnInfo(name = "setup_at") val setupAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_summary_sent_at") val lastSummarySentAt: Long? = null
)
