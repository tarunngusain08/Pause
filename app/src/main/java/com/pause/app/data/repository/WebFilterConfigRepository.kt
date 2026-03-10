package com.pause.app.data.repository

import com.pause.app.data.db.dao.WebFilterConfigDao
import com.pause.app.data.db.entity.WebFilterConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebFilterConfigRepository @Inject constructor(
    private val webFilterConfigDao: WebFilterConfigDao
) {

    suspend fun getConfig(): WebFilterConfig? =
        webFilterConfigDao.getConfig()

    fun getConfigFlow(): Flow<WebFilterConfig?> =
        webFilterConfigDao.getConfigFlow()

    suspend fun saveConfig(config: WebFilterConfig) {
        webFilterConfigDao.insertOrReplace(config)
    }

    suspend fun setVpnEnabled(enabled: Boolean) {
        ensureConfigExists()
        webFilterConfigDao.setVpnEnabled(enabled)
    }

    private suspend fun ensureConfigExists() {
        if (webFilterConfigDao.getConfig() == null) {
            webFilterConfigDao.insertOrReplace(WebFilterConfig(id = 1))
        }
    }
}
