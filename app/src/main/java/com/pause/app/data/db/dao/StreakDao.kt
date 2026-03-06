package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pause.app.data.db.entity.Streak

@Dao
interface StreakDao {

    @Query("SELECT * FROM streaks WHERE id = 1 LIMIT 1")
    suspend fun getStreak(): Streak?

    @Query("SELECT * FROM streaks WHERE id = 1 LIMIT 1")
    fun getStreakFlow(): kotlinx.coroutines.flow.Flow<Streak?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streak: Streak)

    @Update
    suspend fun update(streak: Streak)
}
