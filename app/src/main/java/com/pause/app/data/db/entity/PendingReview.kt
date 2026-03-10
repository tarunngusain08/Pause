package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_review")
data class PendingReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "url_visit_log_id") val urlVisitLogId: Long? = null,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "trigger_keyword") val triggerKeyword: String? = null,
    @ColumnInfo(name = "flagged_at") val flaggedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "status") val status: String, // PENDING | APPROVED | BLOCKED
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long? = null,
    @ColumnInfo(name = "child_note") val childNote: String? = null
)
