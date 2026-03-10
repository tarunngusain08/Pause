package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pause.app.data.db.entity.KeywordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordDao {

    @Query("SELECT * FROM keyword_entries WHERE is_active = 1 ORDER BY category, keyword ASC")
    suspend fun getActiveKeywords(): List<KeywordEntry>

    @Query("SELECT * FROM keyword_entries WHERE is_active = 1 ORDER BY category, keyword ASC")
    fun getActiveKeywordsFlow(): Flow<List<KeywordEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keyword: KeywordEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keywords: List<KeywordEntry>)

    @Query("DELETE FROM keyword_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE keyword_entries SET is_active = :enabled WHERE category = :category")
    suspend fun toggleCategory(category: String, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM keyword_entries WHERE is_bundled = 1")
    suspend fun getBundledCount(): Int
}
