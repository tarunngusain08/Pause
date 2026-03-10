package com.pause.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pause.app.data.db.entity.WebFilterConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface WebFilterConfigDao {

    @Query("SELECT * FROM web_filter_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): WebFilterConfig?

    @Query("SELECT * FROM web_filter_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<WebFilterConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(config: WebFilterConfig)

    @Query("UPDATE web_filter_config SET vpn_enabled = :enabled WHERE id = 1")
    suspend fun setVpnEnabled(enabled: Boolean)
}
