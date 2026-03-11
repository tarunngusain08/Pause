package com.pause.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pause.app.data.db.PauseDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PauseDatabase {
        val dbContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        return Room.databaseBuilder(dbContext, PauseDatabase::class.java, "pause_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // v1 and v2 schema are equivalent; no schema changes needed
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS blacklisted_domains (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    domain TEXT NOT NULL,
                    source TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    added_at INTEGER NOT NULL,
                    added_by TEXT NOT NULL,
                    category TEXT,
                    pending_parent_review INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_blacklisted_domains_domain ON blacklisted_domains(domain)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS whitelisted_domains (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    domain TEXT NOT NULL,
                    added_at INTEGER NOT NULL,
                    reason TEXT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_whitelisted_domains_domain ON whitelisted_domains(domain)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS keyword_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    keyword TEXT NOT NULL,
                    category TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    is_bundled INTEGER NOT NULL DEFAULT 0,
                    added_at INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS url_visit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    full_url TEXT NOT NULL,
                    domain TEXT NOT NULL,
                    browser_package TEXT NOT NULL,
                    visited_at INTEGER NOT NULL,
                    was_blocked INTEGER NOT NULL DEFAULT 0,
                    parent_reviewed INTEGER NOT NULL DEFAULT 0,
                    classification TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS pending_review (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    url_visit_log_id INTEGER,
                    domain TEXT NOT NULL,
                    trigger_keyword TEXT,
                    flagged_at INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    resolved_at INTEGER,
                    child_note TEXT
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS web_filter_config (
                    id INTEGER PRIMARY KEY NOT NULL,
                    vpn_enabled INTEGER NOT NULL DEFAULT 0,
                    url_reader_enabled INTEGER NOT NULL DEFAULT 0,
                    keyword_filter_enabled INTEGER NOT NULL DEFAULT 0,
                    auto_blacklist_on_keyword_match INTEGER NOT NULL DEFAULT 0,
                    notify_parent_on_auto_block INTEGER NOT NULL DEFAULT 0,
                    safe_search_enforcement INTEGER NOT NULL DEFAULT 0,
                    youtube_restricted_mode INTEGER NOT NULL DEFAULT 0,
                    block_incognito INTEGER NOT NULL DEFAULT 0,
                    daily_browsing_budget_minutes INTEGER NOT NULL DEFAULT 0,
                    upstream_dns TEXT NOT NULL DEFAULT '8.8.8.8'
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add unique constraints and indices

            // blacklisted_domains: unique index on domain
            db.execSQL("DROP INDEX IF EXISTS index_blacklisted_domains_domain")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_blacklisted_domains_domain ON blacklisted_domains(domain)")

            // whitelisted_domains: unique index on domain
            db.execSQL("DROP INDEX IF EXISTS index_whitelisted_domains_domain")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_whitelisted_domains_domain ON whitelisted_domains(domain)")

            // keyword_entries: unique index on (keyword, category)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_keyword_entries_keyword_category ON keyword_entries(keyword, category)")

            // sessions: index on is_active
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_is_active ON sessions(is_active)")

            // url_visit_log: indices on visited_at and parent_reviewed
            db.execSQL("CREATE INDEX IF NOT EXISTS index_url_visit_log_visited_at ON url_visit_log(visited_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_url_visit_log_parent_reviewed ON url_visit_log(parent_reviewed)")

            // pending_review: indices on status and flagged_at
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_review_status ON pending_review(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_review_flagged_at ON pending_review(flagged_at)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add composite index on launch_events(package_name, launched_at)
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_launch_events_package_name_launched_at " +
                    "ON launch_events(package_name, launched_at)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideMonitoredAppDao(db: PauseDatabase): MonitoredAppDao = db.monitoredAppDao()

    @Provides
    @Singleton
    fun provideLaunchEventDao(db: PauseDatabase): LaunchEventDao = db.launchEventDao()

    @Provides
    @Singleton
    fun provideSessionDao(db: PauseDatabase): SessionDao = db.sessionDao()

    @Provides
    @Singleton
    fun provideStreakDao(db: PauseDatabase): StreakDao = db.streakDao()

    @Provides
    @Singleton
    fun provideReflectionResponseDao(db: PauseDatabase): ReflectionResponseDao =
        db.reflectionResponseDao()

    @Provides
    @Singleton
    fun provideUnlockEventDao(db: PauseDatabase): UnlockEventDao = db.unlockEventDao()

    @Provides
    @Singleton
    fun provideAccountabilityDao(db: PauseDatabase): AccountabilityDao = db.accountabilityDao()

    @Provides
    @Singleton
    fun provideStrictBreakLogDao(db: PauseDatabase): StrictBreakLogDao = db.strictBreakLogDao()

    @Provides
    @Singleton
    fun provideParentalConfigDao(db: PauseDatabase): ParentalConfigDao = db.parentalConfigDao()

    @Provides
    @Singleton
    fun provideScheduleBandDao(db: PauseDatabase): ScheduleBandDao = db.scheduleBandDao()

    @Provides
    @Singleton
    fun provideParentalBlockedAppDao(db: PauseDatabase): ParentalBlockedAppDao =
        db.parentalBlockedAppDao()

    @Provides
    @Singleton
    fun providePINAuditLogDao(db: PauseDatabase): PINAuditLogDao = db.pinAuditLogDao()

    @Provides
    @Singleton
    fun provideBlacklistedDomainDao(db: PauseDatabase): BlacklistedDomainDao =
        db.blacklistedDomainDao()

    @Provides
    @Singleton
    fun provideWhitelistedDomainDao(db: PauseDatabase): WhitelistedDomainDao =
        db.whitelistedDomainDao()

    @Provides
    @Singleton
    fun provideKeywordDao(db: PauseDatabase): KeywordDao = db.keywordDao()

    @Provides
    @Singleton
    fun provideUrlVisitLogDao(db: PauseDatabase): UrlVisitLogDao = db.urlVisitLogDao()

    @Provides
    @Singleton
    fun providePendingReviewDao(db: PauseDatabase): PendingReviewDao = db.pendingReviewDao()

    @Provides
    @Singleton
    fun provideWebFilterConfigDao(db: PauseDatabase): WebFilterConfigDao = db.webFilterConfigDao()
}
