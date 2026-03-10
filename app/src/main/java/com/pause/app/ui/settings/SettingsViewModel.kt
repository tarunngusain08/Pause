package com.pause.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _delaySeconds = MutableStateFlow(PreferencesManager.DEFAULT_DELAY_SECONDS)
    val delaySeconds: StateFlow<Int> = _delaySeconds.asStateFlow()

    private val _dailyAllowanceMinutes = MutableStateFlow(PreferencesManager.DEFAULT_ALLOWANCE_MINUTES)
    val dailyAllowanceMinutes: StateFlow<Int> = _dailyAllowanceMinutes.asStateFlow()

    private val _currentPhase = MutableStateFlow(PreferencesManager.DEFAULT_PHASE)
    val currentPhase: StateFlow<Int> = _currentPhase.asStateFlow()

    init {
        viewModelScope.launch {
            _delaySeconds.value = preferencesManager.getDelayDurationSeconds()
            _dailyAllowanceMinutes.value = preferencesManager.dailyAllowanceMinutes.first()
            _currentPhase.value = preferencesManager.currentPhase.first()
        }
    }

    fun setDelayDuration(seconds: Int) {
        viewModelScope.launch {
            preferencesManager.setDelayDurationSeconds(seconds)
            _delaySeconds.value = seconds
        }
    }

    fun setDailyAllowanceMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setDailyAllowanceMinutes(minutes)
            _dailyAllowanceMinutes.value = minutes
        }
    }

    fun setCurrentPhase(phase: Int) {
        viewModelScope.launch {
            preferencesManager.setCurrentPhase(phase)
            _currentPhase.value = phase
        }
    }
}
