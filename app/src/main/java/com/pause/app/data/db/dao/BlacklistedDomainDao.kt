package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pause.app.data.db.entity.BlacklistedDomain
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistedDomainDao {

    @Query("SELECT * FROM blacklisted_domains WHERE is_active = 1 ORDER BY added_at ASC")
    fun getActiveDomains(): Flow<List<BlacklistedDomain>>

    @Query("SELECT * FROM blacklisted_domains WHERE is_active = 1 ORDER BY added_at ASC")
    suspend fun getActiveDomainsAsList(): List<BlacklistedDomain>

    @Query("SELECT * FROM blacklisted_domains WHERE domain = :domain LIMIT 1")
    suspend fun getByDomain(domain: String): BlacklistedDomain?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(domain: BlacklistedDomain): Long

    @Update
    suspend fun update(domain: BlacklistedDomain)

    @Query("DELETE FROM blacklisted_domains WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM blacklisted_domains WHERE category = :category AND is_active = 1 ORDER BY added_at ASC")
    suspend fun getByCategory(category: String): List<BlacklistedDomain>

    @Query("SELECT * FROM blacklisted_domains WHERE pending_parent_review = 1 ORDER BY added_at ASC")
    suspend fun getPendingReview(): List<BlacklistedDomain>

    @Query("SELECT COUNT(*) FROM blacklisted_domains WHERE source = 'CATEGORY' AND category = :category AND is_active = 1")
    suspend fun getCategoryDomainCountByCategory(category: String): Int

    @Transaction
    suspend fun insertCategoryDomainsIfNoneForCategory(domains: List<BlacklistedDomain>) {
        val category = domains.firstOrNull()?.category ?: return
        if (getCategoryDomainCountByCategory(category) > 0) return
        domains.forEach { insert(it) }
    }
}
