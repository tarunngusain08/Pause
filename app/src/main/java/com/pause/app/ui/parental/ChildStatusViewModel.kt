package com.pause.app.ui.parental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.service.parental.ScheduleEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildStatusViewModel @Inject constructor(
    private val scheduleEngine: ScheduleEngine
) : ViewModel() {

    private val _currentBand = MutableStateFlow("FREE")
    val currentBand: StateFlow<String> = _currentBand.asStateFlow()

    private var bandPollJob: Job? = null

    init {
        bandPollJob = viewModelScope.launch {
            refreshBand()
            while (isActive) {
                delay(60_000) // Poll every 60 seconds for band boundary changes
                refreshBand()
            }
        }
    }

    private suspend fun refreshBand() {
        val band = scheduleEngine.getCurrentBand()
        _currentBand.update { band.name }
    }

    override fun onCleared() {
        super.onCleared()
        bandPollJob?.cancel()
    }
}
