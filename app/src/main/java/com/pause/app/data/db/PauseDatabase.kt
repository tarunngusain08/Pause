package com.pause.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pause.app.data.db.dao.AccountabilityDao
import com.pause.app.data.db.dao.LaunchEventDao
import com.pause.app.data.db.dao.MonitoredAppDao
import com.pause.app.data.db.dao.ReflectionResponseDao
import com.pause.app.data.db.dao.SessionDao
import com.pause.app.data.db.dao.StreakDao
import com.pause.app.data.db.dao.UnlockEventDao
import com.pause.app.data.db.entity.Accountability
import com.pause.app.data.db.entity.LaunchEvent
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.db.entity.ReflectionResponse
import com.pause.app.data.db.entity.Session
import com.pause.app.data.db.entity.Streak
import com.pause.app.data.db.entity.UnlockEvent

@Database(
    entities = [
        MonitoredApp::class,
        LaunchEvent::class,
        Session::class,
        Streak::class,
        ReflectionResponse::class,
        UnlockEvent::class,
        Accountability::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PauseDatabase : RoomDatabase() {

    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun launchEventDao(): LaunchEventDao
    abstract fun sessionDao(): SessionDao
    abstract fun streakDao(): StreakDao
    abstract fun reflectionResponseDao(): ReflectionResponseDao
    abstract fun unlockEventDao(): UnlockEventDao
    abstract fun accountabilityDao(): AccountabilityDao
}
