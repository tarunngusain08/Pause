package com.pause.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusDuration(val minutes: Int, val label: String)

@HiltViewModel
class FocusModeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val durations = listOf(
        FocusDuration(25, "25 min"),
        FocusDuration(45, "45 min"),
        FocusDuration(60, "1 hour")
    )

    private val _selectedDurationIndex = MutableStateFlow(0)
    val selectedDurationIndex: StateFlow<Int> = _selectedDurationIndex.asStateFlow()

    private val _isStarting = MutableStateFlow(false)
    val isStarting: StateFlow<Boolean> = _isStarting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun selectDuration(index: Int) {
        _selectedDurationIndex.value = index
    }

    fun startSession(onStarted: () -> Unit) {
        viewModelScope.launch {
            _isStarting.value = true
            _error.value = null
            try {
                val duration = durations[_selectedDurationIndex.value].minutes
                sessionRepository.startFocusSession(duration)
                onStarted()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start"
            } finally {
                _isStarting.value = false
            }
        }
    }
}
