package com.pause.app.di

import android.content.Context
import androidx.room.Room
import com.pause.app.data.db.PauseDatabase
import com.pause.app.data.db.dao.AccountabilityDao
import com.pause.app.data.db.dao.LaunchEventDao
import com.pause.app.data.db.dao.MonitoredAppDao
import com.pause.app.data.db.dao.ParentalBlockedAppDao
import com.pause.app.data.db.dao.ParentalConfigDao
import com.pause.app.data.db.dao.PINAuditLogDao
import com.pause.app.data.db.dao.ReflectionResponseDao
import com.pause.app.data.db.dao.ScheduleBandDao
import com.pause.app.data.db.dao.SessionDao
import com.pause.app.data.db.dao.StrictBreakLogDao
import com.pause.app.data.db.dao.StreakDao
import com.pause.app.data.db.dao.UnlockEventDao
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
            .fallbackToDestructiveMigration()
            .build()
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
}
