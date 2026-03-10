package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "url_visit_log")
data class UrlVisitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "full_url") val fullUrl: String, // truncated at 500 chars
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "browser_package") val browserPackage: String,
    @ColumnInfo(name = "visited_at") val visitedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "was_blocked") val wasBlocked: Boolean = false,
    @ColumnInfo(name = "parent_reviewed") val parentReviewed: Boolean = false,
    @ColumnInfo(name = "classification") val classification: String // CLEAN | KEYWORD_MATCH | BLACKLISTED | WHITELISTED
)
