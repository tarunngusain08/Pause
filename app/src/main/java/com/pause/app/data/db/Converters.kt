package com.pause.app.data.db

import androidx.room.TypeConverter
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.db.entity.ParentalBlockedApp
import com.pause.app.data.db.entity.PINAuditLog
import com.pause.app.data.db.entity.ScheduleBandEntity
import com.pause.app.data.db.entity.Session
import com.pause.app.data.db.entity.StrictBreakLog

class Converters {

    @TypeConverter
    fun fromSessionType(value: Session.SessionType): String = value.name

    @TypeConverter
    fun toSessionType(value: String): Session.SessionType =
        Session.SessionType.entries.find { it.name == value } ?: Session.SessionType.FOCUS

    @TypeConverter
    fun fromFrictionLevel(value: MonitoredApp.FrictionLevel): Int = value.value

    @TypeConverter
    fun toFrictionLevel(value: Int): MonitoredApp.FrictionLevel =
        MonitoredApp.FrictionLevel.fromValue(value)

    @TypeConverter
    fun fromBreakReason(value: StrictBreakLog.BreakReason): String = value.name

    @TypeConverter
    fun toBreakReason(value: String): StrictBreakLog.BreakReason =
        StrictBreakLog.BreakReason.entries.find { it.name == value }
            ?: StrictBreakLog.BreakReason.EMERGENCY_EXIT

    @TypeConverter
    fun fromScheduleBandType(value: ScheduleBandEntity.ScheduleBandType): String = value.name

    @TypeConverter
    fun toScheduleBandType(value: String): ScheduleBandEntity.ScheduleBandType =
        ScheduleBandEntity.ScheduleBandType.entries.find { it.name == value }
            ?: ScheduleBandEntity.ScheduleBandType.FREE

    @TypeConverter
    fun fromBlockType(value: ParentalBlockedApp.BlockType): String = value.name

    @TypeConverter
    fun toBlockType(value: String): ParentalBlockedApp.BlockType =
        ParentalBlockedApp.BlockType.entries.find { it.name == value }
            ?: ParentalBlockedApp.BlockType.ALWAYS

    @TypeConverter
    fun fromAttemptSource(value: PINAuditLog.AttemptSource): String = value.name

    @TypeConverter
    fun toAttemptSource(value: String): PINAuditLog.AttemptSource =
        PINAuditLog.AttemptSource.entries.find { it.name == value }
            ?: PINAuditLog.AttemptSource.SETTINGS
}
