package com.pause.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.repository.AppRepository
import com.pause.app.data.repository.LaunchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val launchRepository: LaunchRepository
) : ViewModel() {

    val monitoredApps: StateFlow<List<com.pause.app.data.db.entity.MonitoredApp>> =
        appRepository.getActiveMonitoredApps()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayLaunches: StateFlow<Map<String, Int>> =
        launchRepository.getTodayLaunchesAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
