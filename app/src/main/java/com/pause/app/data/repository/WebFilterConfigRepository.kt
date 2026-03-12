package com.pause.app.data.repository

import android.util.Log
import com.pause.app.data.db.dao.WebFilterConfigDao
import com.pause.app.data.db.entity.WebFilterConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebFilterConfigRepository @Inject constructor(
    private val webFilterConfigDao: WebFilterConfigDao
) {

    suspend fun getConfig(): WebFilterConfig? =
        webFilterConfigDao.getConfig()

    suspend fun saveConfig(config: WebFilterConfig) {
        webFilterConfigDao.insertOrReplace(config)
    }

    suspend fun setVpnEnabled(enabled: Boolean) {
        webFilterConfigDao.ensureAndSetVpnEnabled(enabled)
        val verified = webFilterConfigDao.getConfig()?.vpnEnabled
        if (verified != enabled) {
            Log.w(TAG, "VPN config verification failed: expected vpnEnabled=$enabled, got $verified")
        }
    }

    suspend fun setUrlReaderEnabled(enabled: Boolean) {
        webFilterConfigDao.ensureAndSetUrlReaderEnabled(enabled)
    }

    suspend fun setKeywordFilterEnabled(enabled: Boolean) {
        webFilterConfigDao.ensureAndSetKeywordFilterEnabled(enabled)
    }

    companion object {
        private const val TAG = "WebFilterConfigRepo"
    }
}
