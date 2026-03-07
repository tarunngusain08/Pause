package com.pause.app.ui.appselection

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.MonitoredApp
import com.pause.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val OWN_PACKAGE = "com.pause.app"

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _allInstalledApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())

    val installedApps: StateFlow<List<InstalledAppInfo>> = combine(
        _searchQuery,
        _allInstalledApps
    ) { query, allApps ->
        allApps.filter { app ->
            query.isEmpty() || app.name.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val monitored = withContext(Dispatchers.IO) {
                appRepository.getAllMonitoredApps().first()
            }
            if (monitored.isNotEmpty()) {
                _selectedPackages.value = monitored.map { it.packageName }.toSet()
            }

            val apps = withContext(Dispatchers.IO) {
                // Query apps that appear in the launcher (the app drawer).
                // This correctly includes pre-installed apps (Chrome, YouTube, etc.)
                // while excluding background-only system processes.
                val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(launcherIntent, 0)
                    .map { it.activityInfo.applicationInfo }
                    .distinctBy { it.packageName }
                    .filter { it.packageName != OWN_PACKAGE }
                    .map { appInfo ->
                        InstalledAppInfo(
                            packageName = appInfo.packageName,
                            name = packageManager.getApplicationLabel(appInfo).toString(),
                            icon = appInfo.loadIcon(packageManager)
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }
            _allInstalledApps.value = apps
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleApp(packageName: String) {
        _selectedPackages.value = if (packageName in _selectedPackages.value) {
            _selectedPackages.value - packageName
        } else {
            _selectedPackages.value + packageName
        }
    }

    fun saveSelection(onDone: () -> Unit) {
        viewModelScope.launch {
            val selected = _selectedPackages.value
            val allApps = _allInstalledApps.value
            val apps = selected.map { pkg ->
                val appInfo = allApps.find { it.packageName == pkg }
                MonitoredApp(
                    packageName = pkg,
                    appName = appInfo?.name ?: pkg,
                    appIconUri = null,
                    isActive = true
                )
            }
            appRepository.setMonitoredApps(apps)
            onDone()
        }
    }

    data class InstalledAppInfo(
        val packageName: String,
        val name: String,
        val icon: Drawable
    )
}
