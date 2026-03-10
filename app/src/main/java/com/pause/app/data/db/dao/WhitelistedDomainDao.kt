package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pause.app.data.db.entity.WhitelistedDomain
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedDomainDao {

    @Query("SELECT * FROM whitelisted_domains ORDER BY added_at ASC")
    fun getAll(): Flow<List<WhitelistedDomain>>

    @Query("SELECT * FROM whitelisted_domains ORDER BY added_at ASC")
    suspend fun getAllAsList(): List<WhitelistedDomain>

    @Query("SELECT * FROM whitelisted_domains WHERE domain = :domain LIMIT 1")
    suspend fun getByDomain(domain: String): WhitelistedDomain?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(domain: WhitelistedDomain): Long

    @Query("DELETE FROM whitelisted_domains WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)
}
