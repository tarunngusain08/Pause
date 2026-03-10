package com.pause.app.ui.commitment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.repository.AppRepository
import com.pause.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommitmentModeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val monitoredApps: StateFlow<List<MonitoredApp>> =
        appRepository.getActiveMonitoredApps()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _selectedDurationIndex = MutableStateFlow(0)
    val selectedDurationIndex: StateFlow<Int> = _selectedDurationIndex.asStateFlow()

    private val _isStarting = MutableStateFlow(false)
    val isStarting: StateFlow<Boolean> = _isStarting.asStateFlow()

    val durations = listOf(30, 60, 120) // minutes

    fun toggleApp(packageName: String) {
        _selectedPackages.value = _selectedPackages.value.toMutableSet().apply {
            if (packageName in this) remove(packageName) else add(packageName)
        }
    }

    fun selectDuration(index: Int) {
        _selectedDurationIndex.value = index
    }

    fun startSession(onStarted: () -> Unit) {
        viewModelScope.launch {
            _isStarting.value = true
            val packages = _selectedPackages.value.toList()
            val duration = durations[_selectedDurationIndex.value]
            sessionRepository.startCommitmentSession(duration, packages)
            _isStarting.value = false
            onStarted()
        }
    }
}
