package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pause.app.data.db.entity.ScheduleBandEntity

@Dao
interface ScheduleBandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bands: List<ScheduleBandEntity>)

    @Query("SELECT * FROM schedule_bands WHERE day_of_week = :dayOfWeek ORDER BY start_time ASC")
    suspend fun getBandsForDay(dayOfWeek: Int): List<ScheduleBandEntity>

    @Query("SELECT * FROM schedule_bands ORDER BY day_of_week ASC, start_time ASC")
    suspend fun getAllBands(): List<ScheduleBandEntity>

    @Query("DELETE FROM schedule_bands")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(bands: List<ScheduleBandEntity>) {
        deleteAll()
        if (bands.isNotEmpty()) insertAll(bands)
    }
}
