package com.pause.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pin_audit_log")
data class PINAuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "attempted_at") val attemptedAt: Long,
    @ColumnInfo(name = "was_correct") val wasCorrect: Boolean,
    @ColumnInfo(name = "attempt_source") val attemptSource: AttemptSource
) {
    enum class AttemptSource {
        SETTINGS,
        DISABLE,
        PIN_CHANGE,
        PIN_RESET_VIA_PHRASE
    }
}
