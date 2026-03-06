package com.pause.app.data.repository

import com.pause.app.data.db.dao.StreakDao
import com.pause.app.data.db.entity.Streak
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepository @Inject constructor(
    private val streakDao: StreakDao
) {

    suspend fun getStreak(): Streak? = streakDao.getStreak()

    fun getStreakFlow(): Flow<Streak?> = streakDao.getStreakFlow()

    suspend fun insertOrUpdateStreak(streak: Streak) = streakDao.insert(streak)

    suspend fun updateStreak(streak: Streak) = streakDao.update(streak)
}
