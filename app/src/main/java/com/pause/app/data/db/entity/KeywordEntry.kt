package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keyword_entries")
data class KeywordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "keyword") val keyword: String, // lowercase, normalized
    @ColumnInfo(name = "category") val category: String, // ADULT | VIOLENCE | GAMBLING | HATE | DRUGS | CUSTOM
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "is_bundled") val isBundled: Boolean = false,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)
