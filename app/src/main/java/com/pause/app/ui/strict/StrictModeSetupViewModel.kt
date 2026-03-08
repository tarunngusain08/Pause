package com.pause.app.ui.strict

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.repository.AppRepository
import com.pause.app.service.strict.StrictSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class StrictSetupUiState(
    val monitoredApps: List<MonitoredApp> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    /** Preset duration index (-1 = custom) */
    val selectedPresetIndex: Int = 1,
    val customHours: Int = 0,
    val customMinutes: Int = 30,
    val useCustomDuration: Boolean = false,
    val showFirstConfirm: Boolean = false,
    val showSecondConfirm: Boolean = false,
    val isStarting: Boolean = false,
    val startError: String? = null
) {
    val selectedDurationMs: Long
        get() = if (useCustomDuration) {
            (customHours * 60L + customMinutes) * 60_000L
        } else {
            StrictModeSetupViewModel.DURATION_PRESETS.getOrNull(selectedPresetIndex)?.first
                ?: 60 * 60 * 1000L
        }
}

@HiltViewModel
class StrictModeSetupViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val strictSessionManager: StrictSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrictSetupUiState())
    val uiState: StateFlow<StrictSetupUiState> = _uiState.asStateFlow()

    val activeSession = strictSessionManager.activeSession

    init {
        viewModelScope.launch {
            appRepository.getActiveMonitoredApps().collect { apps ->
                _uiState.update { it.copy(monitoredApps = apps) }
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val newSet = if (packageName in state.selectedPackages) {
                state.selectedPackages - packageName
            } else {
                state.selectedPackages + packageName
            }
            state.copy(selectedPackages = newSet)
        }
    }

    fun selectAllApps() {
        _uiState.update { state ->
            state.copy(selectedPackages = state.monitoredApps.map { it.packageName }.toSet())
        }
    }

    fun selectPreset(index: Int) {
        _uiState.update { it.copy(selectedPresetIndex = index, useCustomDuration = false) }
    }

    fun selectCustomDuration() {
        _uiState.update { it.copy(useCustomDuration = true) }
    }

    fun setCustomHours(hours: Int) {
        _uiState.update { it.copy(customHours = hours.coerceIn(0, 8)) }
    }

    fun setCustomMinutes(minutes: Int) {
        _uiState.update { it.copy(customMinutes = minutes.coerceIn(0, 55)) }
    }

    fun showFirstConfirmation() {
        _uiState.update { it.copy(showFirstConfirm = true) }
    }

    fun dismissFirstConfirmation() {
        _uiState.update { it.copy(showFirstConfirm = false) }
    }

    fun confirmFirstAndShowSecond() {
        _uiState.update { it.copy(showFirstConfirm = false, showSecondConfirm = true) }
    }

    fun dismissSecondConfirmation() {
        _uiState.update { it.copy(showSecondConfirm = false) }
    }

    fun startSession() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.selectedPackages.isEmpty()) {
                _uiState.update { it.copy(startError = "Select at least one app") }
                return@launch
            }
            _uiState.update { it.copy(isStarting = true, showSecondConfirm = false, startError = null) }
            val result = strictSessionManager.startSession(state.selectedDurationMs, state.selectedPackages.toList())
            _uiState.update { it.copy(isStarting = false) }
            result.onFailure { error ->
                _uiState.update { it.copy(startError = error.message ?: "Failed to start session") }
            }
        }
    }

    companion object {
        val DURATION_PRESETS = listOf(
            30 * 60 * 1000L to "30 min",
            60 * 60 * 1000L to "1 hour",
            2 * 60 * 60 * 1000L to "2 hours",
            4 * 60 * 60 * 1000L to "4 hours"
        )
    }
}
