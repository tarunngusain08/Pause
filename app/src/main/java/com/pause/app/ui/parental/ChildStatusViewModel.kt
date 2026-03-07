package com.pause.app.ui.parental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.ScheduleBandEntity
import com.pause.app.service.parental.ScheduleEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildStatusViewModel @Inject constructor(
    private val scheduleEngine: ScheduleEngine
) : ViewModel() {

    private val _currentBand = MutableStateFlow("FREE")
    val currentBand: StateFlow<String> = _currentBand.asStateFlow()

    init {
        viewModelScope.launch {
            val band = scheduleEngine.getCurrentBand()
            _currentBand.update { band.name }
        }
    }
}
