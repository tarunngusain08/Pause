package com.pause.app.ui.parental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.ParentalConfig
import com.pause.app.data.preferences.PreferencesManager
import com.pause.app.data.preferences.SessionPreferences
import com.pause.app.data.repository.ParentalConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentalSetupViewModel @Inject constructor(
    private val parentalConfigRepository: ParentalConfigRepository,
    private val sessionPreferences: SessionPreferences,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val currentStep: StateFlow<Int> = preferencesManager.parentalSetupStep
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSteps = 5

    fun advanceStep() {
        viewModelScope.launch {
            val next = (currentStep.value + 1).coerceAtMost(totalSteps - 1)
            preferencesManager.setParentalSetupStep(next)
        }
    }

    fun completeSetup(onComplete: () -> Unit) {
        viewModelScope.launch {
            val config = parentalConfigRepository.getConfigSync()
            if (config == null) {
                parentalConfigRepository.saveConfig(
                    ParentalConfig(
                        id = 1,
                        isActive = true,
                        setupAt = System.currentTimeMillis(),
                        lastModifiedAt = System.currentTimeMillis()
                    )
                )
            } else {
                parentalConfigRepository.setActive(true)
            }
            sessionPreferences.parentalActive = true
            preferencesManager.clearParentalSetupStep()
            onComplete()
        }
    }
}
