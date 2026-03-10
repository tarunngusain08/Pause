package com.pause.app.ui.webfilter

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.repository.UrlVisitLogRepository
import com.pause.app.data.repository.WebFilterConfigRepository
import com.pause.app.service.webfilter.PauseVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebFilterDashboardUiState(
    val vpnEnabled: Boolean = false,
    val domainsVisited: Int = 0,
    val domainsBlocked: Int = 0,
    val keywordMatches: Int = 0
)

@HiltViewModel
class WebFilterDashboardViewModel @Inject constructor(
    private val webFilterConfigRepository: WebFilterConfigRepository,
    private val urlVisitLogRepository: UrlVisitLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebFilterDashboardUiState())
    val uiState: StateFlow<WebFilterDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            webFilterConfigRepository.getConfigFlow().collect { config ->
                _uiState.update {
                    it.copy(vpnEnabled = config?.vpnEnabled == true)
                }
            }
        }
        viewModelScope.launch {
            val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            val stats = urlVisitLogRepository.getDailyStats(since)
            _uiState.update {
                it.copy(
                    domainsVisited = stats.visitCount,
                    domainsBlocked = stats.blockedCount,
                    keywordMatches = stats.keywordMatchCount
                )
            }
        }
    }

    fun setVpnEnabled(enabled: Boolean) {
        viewModelScope.launch {
            webFilterConfigRepository.setVpnEnabled(enabled)
        }
    }

    fun onResume(context: Context) {
        viewModelScope.launch {
            val config = webFilterConfigRepository.getConfig()
            if (config?.vpnEnabled == true && VpnService.prepare(context) == null) {
                val intent = Intent(context, PauseVpnService::class.java)
                    .setAction(PauseVpnService.ACTION_START)
                context.startForegroundService(intent)
            }
        }
    }
}
