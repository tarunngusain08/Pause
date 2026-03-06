package com.pause.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accountability")
data class Accountability(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partnerContact: String,
    val partnerName: String,
    val partnerAccepted: Boolean = false,
    val setupAt: Long = System.currentTimeMillis(),
    val lastSummarySentAt: Long? = null
)
