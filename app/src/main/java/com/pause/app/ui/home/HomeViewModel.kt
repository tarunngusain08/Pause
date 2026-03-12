package com.pause.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import com.pause.app.data.db.entity.Session
import com.pause.app.service.strict.StrictSessionManager
import com.pause.app.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val strictSessionManager: StrictSessionManager
) : ViewModel() {

    val strictSession: StateFlow<Session?> = strictSessionManager.activeSession

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
