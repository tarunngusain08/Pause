package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blacklisted_domains",
    indices = [Index(value = ["domain"])]
)
data class BlacklistedDomain(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "source") val source: String, // MANUAL | AUTO_KEYWORD | CATEGORY | SEED_LIST
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "added_by") val addedBy: String, // PARENT | SYSTEM | AUTO
    @ColumnInfo(name = "category") val category: String? = null, // ADULT | VIOLENCE | GAMBLING | CUSTOM | null
    @ColumnInfo(name = "pending_parent_review") val pendingParentReview: Boolean = false
)
