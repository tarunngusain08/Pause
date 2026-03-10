package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelisted_domains",
    indices = [Index(value = ["domain"])]
)
data class WhitelistedDomain(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "reason") val reason: String? = null
)
