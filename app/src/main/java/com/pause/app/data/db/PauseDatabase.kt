package com.pause.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pause.app.data.db.dao.AccountabilityDao
import com.pause.app.data.db.dao.BlacklistedDomainDao
import com.pause.app.data.db.dao.KeywordDao
import com.pause.app.data.db.dao.LaunchEventDao
import com.pause.app.data.db.dao.MonitoredAppDao
import com.pause.app.data.db.dao.ParentalBlockedAppDao
import com.pause.app.data.db.dao.ParentalConfigDao
import com.pause.app.data.db.dao.PendingReviewDao
import com.pause.app.data.db.dao.PINAuditLogDao
import com.pause.app.data.db.dao.ReflectionResponseDao
import com.pause.app.data.db.dao.ScheduleBandDao
import com.pause.app.data.db.dao.SessionDao
import com.pause.app.data.db.dao.StrictBreakLogDao
import com.pause.app.data.db.dao.StreakDao
import com.pause.app.data.db.dao.UnlockEventDao
import com.pause.app.data.db.dao.UrlVisitLogDao
import com.pause.app.data.db.dao.WebFilterConfigDao
import com.pause.app.data.db.dao.WhitelistedDomainDao
import com.pause.app.data.db.entity.Accountability
import com.pause.app.data.db.entity.BlacklistedDomain
import com.pause.app.data.db.entity.KeywordEntry
import com.pause.app.data.db.entity.LaunchEvent
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.db.entity.ParentalBlockedApp
import com.pause.app.data.db.entity.ParentalConfig
import com.pause.app.data.db.entity.PendingReview
import com.pause.app.data.db.entity.PINAuditLog
import com.pause.app.data.db.entity.ReflectionResponse
import com.pause.app.data.db.entity.ScheduleBandEntity
import com.pause.app.data.db.entity.Session
import com.pause.app.data.db.entity.StrictBreakLog
import com.pause.app.data.db.entity.Streak
import com.pause.app.data.db.entity.UnlockEvent
import com.pause.app.data.db.entity.UrlVisitLog
import com.pause.app.data.db.entity.WebFilterConfig
import com.pause.app.data.db.entity.WhitelistedDomain

@Database(
    entities = [
        MonitoredApp::class,
        LaunchEvent::class,
        Session::class,
        Streak::class,
        ReflectionResponse::class,
        UnlockEvent::class,
        Accountability::class,
        StrictBreakLog::class,
        ParentalConfig::class,
        ScheduleBandEntity::class,
        ParentalBlockedApp::class,
        PINAuditLog::class,
        BlacklistedDomain::class,
        WhitelistedDomain::class,
        KeywordEntry::class,
        UrlVisitLog::class,
        PendingReview::class,
        WebFilterConfig::class
    ],
    version = 3,
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
    abstract fun strictBreakLogDao(): StrictBreakLogDao
    abstract fun parentalConfigDao(): ParentalConfigDao
    abstract fun scheduleBandDao(): ScheduleBandDao
    abstract fun parentalBlockedAppDao(): ParentalBlockedAppDao
    abstract fun pinAuditLogDao(): PINAuditLogDao
    abstract fun blacklistedDomainDao(): BlacklistedDomainDao
    abstract fun whitelistedDomainDao(): WhitelistedDomainDao
    abstract fun keywordDao(): KeywordDao
    abstract fun urlVisitLogDao(): UrlVisitLogDao
    abstract fun pendingReviewDao(): PendingReviewDao
    abstract fun webFilterConfigDao(): WebFilterConfigDao
}
