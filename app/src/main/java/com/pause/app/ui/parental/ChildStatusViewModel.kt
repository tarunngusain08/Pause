package com.pause.app.ui.parental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.ParentalBlockedApp
import com.pause.app.data.repository.ParentalBlockedAppRepository
import com.pause.app.data.repository.UrlVisitLogRepository
import com.pause.app.service.parental.ScheduleEngine
import com.pause.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class InternetSummary(
    val totalVisits: Int,
    val blockedVisits: Int,
    val keywordFlags: Int
)

@HiltViewModel
class ChildStatusViewModel @Inject constructor(
    private val scheduleEngine: ScheduleEngine,
    private val blockedAppRepository: ParentalBlockedAppRepository,
    private val urlVisitLogRepository: UrlVisitLogRepository
) : ViewModel() {

    private val _currentBand = MutableStateFlow("FREE")
    val currentBand: StateFlow<String> = _currentBand.asStateFlow()

    private val _nextBandChange = MutableStateFlow<String?>(null)
    val nextBandChange: StateFlow<String?> = _nextBandChange.asStateFlow()

    val blockedApps: StateFlow<List<ParentalBlockedApp>> =
        blockedAppRepository.getActiveBlockedApps()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _internetSummary = MutableStateFlow<InternetSummary?>(null)
    val internetSummary: StateFlow<InternetSummary?> = _internetSummary.asStateFlow()

    private var bandPollJob: Job? = null

    init {
        bandPollJob = viewModelScope.launch {
            refreshBand()
            refreshInternetSummary()
            while (isActive) {
                delay(60_000)
                refreshBand()
                refreshInternetSummary()
            }
        }
    }

    private suspend fun refreshBand() {
        val band = scheduleEngine.getCurrentBand()
        _currentBand.update { band.name }
        val next = scheduleEngine.getNextBandChange()
        _nextBandChange.update {
            next?.let { bc ->
                val formatter = SimpleDateFormat("EEE, h:mm a", Locale.getDefault())
                "${bc.newBand.name} at ${formatter.format(Date(bc.changeAt))}"
            }
        }
    }

    private suspend fun refreshInternetSummary() {
        val since = DateUtils.getTodayMidnight()
        val stats = urlVisitLogRepository.getDailyStats(since)
        _internetSummary.update {
            InternetSummary(
                totalVisits = stats.visitCount,
                blockedVisits = stats.blockedCount,
                keywordFlags = stats.keywordMatchCount
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        bandPollJob?.cancel()
    }
}
