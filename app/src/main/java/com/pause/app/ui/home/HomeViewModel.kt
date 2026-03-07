package com.pause.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.db.entity.Session
import com.pause.app.data.repository.AppRepository
import com.pause.app.data.repository.LaunchRepository
import com.pause.app.service.strict.StrictSessionManager
import com.pause.app.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PermissionStatus(
    val overlayGranted: Boolean = true,
    val accessibilityEnabled: Boolean = true
) {
    val allGranted: Boolean get() = overlayGranted && accessibilityEnabled
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val launchRepository: LaunchRepository,
    private val strictSessionManager: StrictSessionManager
) : ViewModel() {

    val monitoredApps: StateFlow<List<MonitoredApp>> =
        appRepository.getActiveMonitoredApps()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayLaunches: StateFlow<Map<String, Int>> =
        launchRepository.getTodayLaunchesAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val strictSession: StateFlow<Session?> =
        strictSessionManager.activeSession

    private val _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    fun refreshPermissions() {
        _permissionStatus.value = PermissionStatus(
            overlayGranted = PermissionHelper.hasOverlayPermission(context),
            accessibilityEnabled = PermissionHelper.hasAccessibilityServiceEnabled(context)
        )
    }

    fun getStrictRemainingMs(): Long = strictSessionManager.getRemainingMs()
}
