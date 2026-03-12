package com.pause.app.ui.strict

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.service.strict.StrictSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StrictSetupUiState(
    /** Preset duration index (0–3 for 4 presets) */
    val selectedPresetIndex: Int = 1,
    val customMinutesText: String = "30",
    val useCustomDuration: Boolean = false,
    val showFirstConfirm: Boolean = false,
    val showSecondConfirm: Boolean = false,
    val isStarting: Boolean = false,
    val startError: String? = null
) {
    private fun parseCustomMinutes(): Int {
        val parsed = customMinutesText.filter { it.isDigit() }.toIntOrNull() ?: 0
        return parsed.coerceIn(0, 480)
    }

    val selectedDurationMs: Long
        get() = if (useCustomDuration) {
            parseCustomMinutes().toLong() * 60_000L
        } else {
            StrictModeSetupViewModel.DURATION_PRESETS.getOrNull(selectedPresetIndex)?.first
                ?: 60 * 60 * 1000L
        }
}

@HiltViewModel
class StrictModeSetupViewModel @Inject constructor(
    private val strictSessionManager: StrictSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrictSetupUiState())
    val uiState: StateFlow<StrictSetupUiState> = _uiState.asStateFlow()

    val activeSession = strictSessionManager.activeSession

    fun selectPreset(index: Int) {
        _uiState.update { it.copy(selectedPresetIndex = index, useCustomDuration = false) }
    }

    fun selectCustomDuration() {
        _uiState.update { it.copy(useCustomDuration = true) }
    }

    fun setCustomMinutesText(text: String) {
        val filtered = text.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(customMinutesText = filtered) }
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
            _uiState.update { it.copy(isStarting = true, showSecondConfirm = false, startError = null) }
            val result = strictSessionManager.startSession(state.selectedDurationMs)
            _uiState.update { it.copy(isStarting = false) }
            result.onFailure { error ->
                _uiState.update { it.copy(startError = error.message ?: "Failed to start session") }
            }
        }
    }

    companion object {
        val DURATION_PRESETS = listOf(
            15 * 60 * 1000L to "15 min",
            30 * 60 * 1000L to "30 min",
            60 * 60 * 1000L to "1 hr",
            2 * 60 * 60 * 1000L to "2 hr"
        )
    }
}
