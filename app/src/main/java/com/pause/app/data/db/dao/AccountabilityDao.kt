package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pause.app.data.db.entity.Accountability
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountabilityDao {

    @Query("SELECT * FROM accountability LIMIT 1")
    suspend fun getPartner(): Accountability?

    @Query("SELECT * FROM accountability LIMIT 1")
    fun getPartnerFlow(): Flow<Accountability?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accountability: Accountability): Long

    @Update
    suspend fun update(accountability: Accountability)

    @Query("DELETE FROM accountability")
    suspend fun deleteAll()
}
