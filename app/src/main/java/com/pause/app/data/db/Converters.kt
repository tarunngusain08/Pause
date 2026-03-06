package com.pause.app.data.db

import androidx.room.TypeConverter
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.db.entity.Session

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
}
