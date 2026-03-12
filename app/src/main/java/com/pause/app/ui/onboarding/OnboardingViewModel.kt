package com.pause.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.preferences.PreferencesManager
import com.pause.app.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME,
    OVERLAY,
    ACCESSIBILITY,
    NOTIFICATIONS,
    DONE
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _onboardingComplete = MutableStateFlow(false)
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _hasOverlay = MutableStateFlow(false)
    val hasOverlay: StateFlow<Boolean> = _hasOverlay.asStateFlow()

    private val _hasAccessibility = MutableStateFlow(false)
    val hasAccessibility: StateFlow<Boolean> = _hasAccessibility.asStateFlow()

    init {
        viewModelScope.launch {
            _onboardingComplete.value = preferencesManager.onboardingComplete.first()
            _isReady.value = true
        }
        // Eagerly populate permission state so the UI is correct on first display
        // (ON_RESUME DisposableEffect may not fire before first composition).
        refreshPermissions()
    }

    fun refreshPermissions() {
        _hasOverlay.value = PermissionHelper.hasOverlayPermission(context)
        _hasAccessibility.value = PermissionHelper.hasAccessibilityServiceEnabled(context)
    }

    fun advanceFromWelcome() {
        _currentStep.value = OnboardingStep.OVERLAY
    }

    fun advanceFromOverlay() {
        _currentStep.value = OnboardingStep.ACCESSIBILITY
    }

    fun advanceFromAccessibility() {
        _currentStep.value = if (PermissionHelper.needsNotificationPermission()) {
            OnboardingStep.NOTIFICATIONS
        } else {
            OnboardingStep.DONE
        }
    }

    fun advanceFromNotifications() {
        _currentStep.value = OnboardingStep.DONE
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingComplete(true)
            _onboardingComplete.value = true
        }
    }
}
